
package com.payflow.service;

import com.payflow.dto.refund.RefundRequest;
import com.payflow.dto.refund.RefundResponse;
import com.payflow.entity.Payment;
import com.payflow.entity.PaymentStatus;
import com.payflow.entity.Refund;
import com.payflow.entity.RefundStatus;
import com.payflow.repository.PaymentRepository;
import com.payflow.repository.RefundRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RazorpayService razorpayService;
    private final NotificationService notificationService;

    public RefundService(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            RazorpayService razorpayService,
            NotificationService notificationService) {

        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.razorpayService = razorpayService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RefundResponse createRefund(
            UUID merchantId,
            UUID paymentId,
            RefundRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null
                || !idempotencyKey.matches("[A-Za-z0-9_-]{16,100}")) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 16-100 letters, digits, _ or -"
            );
        }

        var existing = refundRepository
                .findByMerchantIdAndIdempotencyKey(
                        merchantId, idempotencyKey);

        if (existing.isPresent()) {
            Refund previous = existing.get();

            if (!previous.getPayment().getId().equals(paymentId)
                    || request.amount() == null
                    || previous.getAmount().compareTo(request.amount()) != 0) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for another request"
                );
            }

            return RefundResponse.from(previous);
        }

        Payment payment = paymentRepository.findWithLockById(paymentId)
                .filter(p -> p.getMerchantId().equals(merchantId))
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCESS
                || payment.getRazorpayPaymentId() == null
                || payment.getRazorpayPaymentId().isBlank()) {
            throw new IllegalArgumentException(
                    "Only successful payments can be refunded"
            );
        }

        BigDecimal amount = request.amount();

        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Refund amount must be positive with at most 2 decimals"
            );
        }

        List<Refund> previousRefunds =
                refundRepository.findByPayment_Id(paymentId);

        BigDecimal reserved = previousRefunds.stream()
                .filter(r -> r.getStatus() != RefundStatus.FAILED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = payment.getAmount().subtract(reserved);

        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Refund exceeds remaining refundable amount: " + remaining
            );
        }

        String reason = request.reason() == null
                ? null : request.reason().trim();

        Refund refund = new Refund(
                payment, merchantId, idempotencyKey, amount, reason);

        // Persist refund reservation before calling Razorpay.
        refund = refundRepository.saveAndFlush(refund);

        try {
            long amountInPaise = amount
                    .movePointRight(2)
                    .longValueExact();

            com.razorpay.Refund providerRefund =
                    razorpayService.createRefund(
                            payment.getRazorpayPaymentId(),
                            amountInPaise,
                            refund.getId().toString());

            refund.setRazorpayRefundId(providerRefund.get("id"));

            String providerStatus = providerRefund.get("status");
            refund.setStatus(mapStatus(providerStatus));

            refundRepository.save(refund);

        } catch (Exception exception) {
            // Keep PENDING if the provider outcome is uncertain.
            // Reconcile this refund through the provider webhook/API.
            refundRepository.save(refund);
        }

        notificationService.create(
                merchantId,
                "Refund Initiated",
                "A refund of " + payment.getCurrency()
                        + " " + amount
                        + " has been initiated. Current status: "
                        + refund.getStatus().name() + ".",
                "REFUND"
        );

        return RefundResponse.from(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefunds(
            UUID merchantId,
            UUID paymentId) {

        // Verify payment ownership before exposing refund details.
        paymentRepository.findById(paymentId)
                .filter(p -> p.getMerchantId().equals(merchantId))
                .orElseThrow(() ->
                        new IllegalArgumentException("Payment not found"));

        return refundRepository
                .findByPayment_IdAndMerchantIdOrderByCreatedAtDesc(
                        paymentId, merchantId)
                .stream()
                .map(RefundResponse::from)
                .toList();
    }

    @Transactional
    public void updateRefundStatus(
            String razorpayRefundId,
            String status) {

        Refund refund = refundRepository
                .findByRazorpayRefundId(razorpayRefundId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Refund not found"));

        RefundStatus currentStatus = refund.getStatus();
        RefundStatus next = mapStatus(status);

        // Ignore duplicate or delayed events after terminal status.
        if (currentStatus == RefundStatus.PROCESSED
                || currentStatus == RefundStatus.FAILED) {
            return;
        }

        refund.setStatus(next);
        refundRepository.save(refund);

        if (currentStatus != next) {
            notificationService.create(
                    refund.getMerchantId(),
                    "Refund Status Updated",
                    "Your refund of "
                            + refund.getPayment().getCurrency()
                            + " " + refund.getAmount()
                            + " is now " + next.name() + ".",
                    "REFUND"
            );
        }
    }

    private RefundStatus mapStatus(String status) {
        if (status == null) {
            return RefundStatus.PENDING;
        }

        return switch (status.toLowerCase(Locale.ROOT)) {
            case "processed" -> RefundStatus.PROCESSED;
            case "failed" -> RefundStatus.FAILED;
            default -> RefundStatus.PENDING;
        };
    }
}
