
package com.payflow.service;

import com.payflow.dto.payment.PaymentRequest;
import com.payflow.dto.payment.PaymentResponse;
import com.payflow.dto.payment.RazorpayCheckoutResponse;
import com.payflow.dto.payment.RazorpayVerifyRequest;
import com.payflow.entity.Payment;
import com.payflow.entity.PaymentStatus;
import com.payflow.repository.PaymentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final NotificationService notificationService;
    private final String razorpayKeyId;

    public PaymentService(
            PaymentRepository paymentRepository,
            RazorpayService razorpayService,
            NotificationService notificationService,
            @Value("${razorpay.key.id}") String razorpayKeyId) {

        this.paymentRepository = paymentRepository;
        this.razorpayService = razorpayService;
        this.notificationService = notificationService;
        this.razorpayKeyId = razorpayKeyId;
    }

    // Create local payment record and Razorpay order
    @Transactional
    public RazorpayCheckoutResponse createPayment(
            UUID merchantId,
            PaymentRequest request) {

        Payment payment = new Payment();

        payment.setMerchantId(merchantId);
        payment.setAmount(request.amount());
        payment.setCurrency(
                request.currency().trim().toUpperCase(Locale.ROOT)
        );
        payment.setDescription(request.description().trim());
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        try {
            String orderId = razorpayService.createOrder(
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getId()
            );

            savedPayment.setRazorpayOrderId(orderId);
            paymentRepository.save(savedPayment);

            return new RazorpayCheckoutResponse(
                    savedPayment.getId(),
                    orderId,
                    razorpayKeyId,
                    savedPayment.getAmount(),
                    savedPayment.getCurrency(),
                    savedPayment.getStatus().name(),
                    savedPayment.getDescription()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to create Razorpay order. Please try again.",
                    exception
            );
        }
    }

    // Verify payment for the authenticated merchant
    @Transactional
    public PaymentResponse verifyPayment(
            UUID merchantId,
            RazorpayVerifyRequest request) {

        Payment payment = paymentRepository
                .findWithLockByRazorpayOrderId(request.razorpayOrderId())
                .filter(p -> p.getMerchantId().equals(merchantId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for this Razorpay order"
                ));

        // Avoid duplicate processing and notifications
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (request.razorpayPaymentId().equals(
                    payment.getRazorpayPaymentId())) {
                return PaymentResponse.from(payment);
            }

            throw new IllegalArgumentException(
                    "Payment already completed with a different payment ID"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Payment is not in a verifiable state"
            );
        }

        try {
            boolean validSignature =
                    razorpayService.verifyPaymentSignature(
                            request.razorpayOrderId(),
                            request.razorpayPaymentId(),
                            request.razorpaySignature()
                    );

            if (!validSignature) {
                throw new IllegalArgumentException(
                        "Invalid Razorpay payment signature"
                );
            }

            long expectedAmountInPaise = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            boolean captured = razorpayService.isPaymentCaptured(
                    request.razorpayPaymentId(),
                    request.razorpayOrderId(),
                    expectedAmountInPaise,
                    payment.getCurrency()
            );

            if (!captured) {
                throw new IllegalStateException(
                        "Razorpay has not confirmed this payment as captured"
                );
            }

            payment.setRazorpayPaymentId(request.razorpayPaymentId());
            payment.setFailureReason(null);
            payment.setStatus(PaymentStatus.SUCCESS);

            Payment savedPayment = paymentRepository.save(payment);

            notificationService.create(
                    merchantId,
                    "Payment Successful",
                    "Your payment of " + savedPayment.getCurrency()
                            + " " + savedPayment.getAmount()
                            + " was successful.",
                    "PAYMENT"
            );

            return PaymentResponse.from(savedPayment);

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to verify Razorpay payment",
                    exception
            );
        }
    }

    // Process a captured-payment webhook after signature verification
    @Transactional
    public void handleCapturedWebhook(
            String razorpayOrderId,
            String razorpayPaymentId) {

        Payment payment = paymentRepository
                .findWithLockByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for Razorpay order"
                ));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (razorpayPaymentId.equals(payment.getRazorpayPaymentId())) {
                return;
            }

            throw new IllegalStateException(
                    "Order is already successful with a different payment ID"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not in a processable state"
            );
        }

        try {
            long expectedAmountInPaise = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            boolean captured = razorpayService.isPaymentCaptured(
                    razorpayPaymentId,
                    razorpayOrderId,
                    expectedAmountInPaise,
                    payment.getCurrency()
            );

            if (!captured) {
                throw new IllegalStateException(
                        "Razorpay payment is not confirmed as captured"
                );
            }

            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setFailureReason(null);
            payment.setStatus(PaymentStatus.SUCCESS);

            Payment savedPayment = paymentRepository.save(payment);

            notificationService.create(
                    savedPayment.getMerchantId(),
                    "Payment Successful",
                    "Your payment of " + savedPayment.getCurrency()
                            + " " + savedPayment.getAmount()
                            + " was confirmed successfully.",
                    "PAYMENT"
            );

        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to process captured payment webhook",
                    exception
            );
        }
    }

    // Process a verified payment.failed webhook
    @Transactional
    public void handleFailedWebhook(
            String razorpayOrderId,
            String razorpayPaymentId,
            String failureReason) {

        Payment payment = paymentRepository
                .findWithLockByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for Razorpay order"
                ));

        // Never overwrite a successful payment
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        // Process only pending payments
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);

        String safeReason = failureReason == null
                || failureReason.isBlank()
                ? "Payment failed"
                : failureReason.substring(
                0, Math.min(failureReason.length(), 500)
        );

        payment.setFailureReason(safeReason);
        payment.setStatus(PaymentStatus.FAILED);

        paymentRepository.save(payment);

        notificationService.create(
                payment.getMerchantId(),
                "Payment Failed",
                "Your payment of " + payment.getCurrency()
                        + " " + payment.getAmount()
                        + " failed. Reason: " + safeReason,
                "PAYMENT"
        );
    }

    // Payment history without status filter
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(
            UUID merchantId,
            int page,
            int size) {

        return getPaymentHistory(merchantId, page, size, null);
    }

    // Payment history with optional status filter
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentHistory(
            UUID merchantId,
            int page,
            int size,
            PaymentStatus status) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> payments;

        if (status == null) {
            payments = paymentRepository
                    .findByMerchantIdOrderByCreatedAtDesc(
                            merchantId, pageable
                    );
        } else {
            payments = paymentRepository
                    .findByMerchantIdAndStatusOrderByCreatedAtDesc(
                            merchantId, status, pageable
                    );
        }

        return payments.map(PaymentResponse::from);
    }

    // Payment dashboard summary
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentSummary(UUID merchantId) {

        long totalPayments =
                paymentRepository.countByMerchantId(merchantId);

        long successfulPayments =
                paymentRepository.countByMerchantIdAndStatus(
                        merchantId, PaymentStatus.SUCCESS
                );

        long pendingPayments =
                paymentRepository.countByMerchantIdAndStatus(
                        merchantId, PaymentStatus.PENDING
                );

        long failedPayments =
                paymentRepository.countByMerchantIdAndStatus(
                        merchantId, PaymentStatus.FAILED
                );

        BigDecimal totalAmount =
                paymentRepository.sumAmountByMerchantId(merchantId);

        BigDecimal successfulAmount =
                paymentRepository.sumAmountByMerchantIdAndStatus(
                        merchantId, PaymentStatus.SUCCESS
                );

        return Map.of(
                "totalPayments", totalPayments,
                "successfulPayments", successfulPayments,
                "pendingPayments", pendingPayments,
                "failedPayments", failedPayments,
                "totalAmount", totalAmount != null
                        ? totalAmount : BigDecimal.ZERO,
                "successfulAmount", successfulAmount != null
                        ? successfulAmount : BigDecimal.ZERO
        );
    }

    // Fetch a payment belonging to this merchant
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(
            UUID merchantId,
            UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .filter(p -> p.getMerchantId().equals(merchantId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found"
                ));

        return PaymentResponse.from(payment);
    }
}
