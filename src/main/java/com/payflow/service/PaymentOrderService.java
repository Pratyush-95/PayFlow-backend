
package com.payflow.service;

import com.payflow.PaymentOrderCreationResult;
import com.payflow.entity.Merchant;
import com.payflow.entity.PaymentOrder;
import com.payflow.repository.MerchantRepository;
import com.payflow.repository.PaymentOrderRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final MerchantRepository merchantRepository;

    public PaymentOrderService(
            PaymentOrderRepository paymentOrderRepository,
            MerchantRepository merchantRepository) {

        this.paymentOrderRepository = paymentOrderRepository;
        this.merchantRepository = merchantRepository;
    }

    // Create payment order with merchant and idempotency
    public PaymentOrderCreationResult createPaymentOrder(
            PaymentOrder paymentOrder,
            String idempotencyKey,
            UUID merchantId) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key is required");
        }

        if (idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed 100 characters");
        }

        if (merchantId == null) {
            throw new IllegalArgumentException(
                    "Merchant ID is required");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Merchant not found: " + merchantId));

        var existingOrder =
                paymentOrderRepository.findByIdempotencyKey(idempotencyKey);

        if (existingOrder.isPresent()) {
            return getExistingOrderResult(
                    existingOrder.get(), paymentOrder, merchantId);
        }

        paymentOrder.setMerchant(merchant);
        paymentOrder.setIdempotencyKey(idempotencyKey);

        try {
            PaymentOrder savedOrder =
                    paymentOrderRepository.saveAndFlush(paymentOrder);

            return new PaymentOrderCreationResult(savedOrder, true);

        } catch (DataIntegrityViolationException ex) {
            var concurrentOrder =
                    paymentOrderRepository.findByIdempotencyKey(
                            idempotencyKey);

            if (concurrentOrder.isPresent()) {
                return getExistingOrderResult(
                        concurrentOrder.get(), paymentOrder, merchantId);
            }

            throw ex;
        }
    }

    // Check whether an existing order matches the request
    private PaymentOrderCreationResult getExistingOrderResult(
            PaymentOrder existingOrder,
            PaymentOrder requestedOrder,
            UUID merchantId) {

        boolean sameAmount =
                existingOrder.getAmount()
                        .compareTo(requestedOrder.getAmount()) == 0;

        boolean sameCurrency =
                existingOrder.getCurrency()
                        .equals(requestedOrder.getCurrency());

        boolean sameMerchant =
                existingOrder.getMerchant() != null
                        && existingOrder.getMerchant().getId()
                        .equals(merchantId);

        if (sameAmount && sameCurrency && sameMerchant) {
            return new PaymentOrderCreationResult(existingOrder, false);
        }

        throw new IllegalArgumentException(
                "Idempotency-Key was already used with different request data");
    }

    // Fetch payment order by ID
    public PaymentOrder getPaymentOrderById(UUID id) {
        return paymentOrderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment order not found: " + id));
    }

    // Fetch all payment orders
    public List<PaymentOrder> getAllPaymentOrders() {
        return paymentOrderRepository.findAll();
    }

    // Fetch paginated payment orders for a merchant,
    // optionally filtered by status
    public Page<PaymentOrder> getPaymentOrdersByMerchantId(
            UUID merchantId,
            String status,
            Pageable pageable) {

        if (status == null || status.isBlank()) {
            return paymentOrderRepository.findByMerchant_Id(
                    merchantId, pageable);
        }

        String normalizedStatus = status.trim().toUpperCase();

        if (!List.of("CREATED", "SUCCESS", "FAILED")
                .contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Status must be CREATED, SUCCESS or FAILED");
        }

        return paymentOrderRepository.findByMerchant_IdAndStatus(
                merchantId, normalizedStatus, pageable);
    }

    // Fetch orders using a period or custom date range
    public Page<PaymentOrder> getPaymentOrdersByDateRange(
            UUID merchantId,
            String period,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Pageable pageable) {

        if ((period == null || period.isBlank())
                && startDate == null && endDate == null) {
            return getPaymentOrdersByMerchantId(
                    merchantId, status, pageable);
        }

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (period != null && !period.isBlank()) {

            if (startDate != null || endDate != null) {
                throw new IllegalArgumentException(
                        "Use period or custom dates, not both");
            }

            LocalDateTime now = LocalDateTime.now();

            switch (period.trim().toLowerCase()) {
                case "24h":
                    startDateTime = now.minusHours(24);
                    endDateTime = now;
                    break;

                case "7d":
                    startDateTime = now.minusDays(7);
                    endDateTime = now;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Period must be 24h or 7d");
            }

        } else {
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException(
                        "Both startDate and endDate are required");
            }

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException(
                        "startDate cannot be after endDate");
            }

            startDateTime = startDate.atStartOfDay();

            // Exclusive end includes the complete end date
            endDateTime = endDate.plusDays(1).atStartOfDay();
        }

        String normalizedStatus = null;

        if (status != null && !status.isBlank()) {
            normalizedStatus = status.trim().toUpperCase();

            if (!List.of("CREATED", "SUCCESS", "FAILED")
                    .contains(normalizedStatus)) {
                throw new IllegalArgumentException(
                        "Status must be CREATED, SUCCESS or FAILED");
            }
        }

        if (normalizedStatus == null) {
            return paymentOrderRepository
                    .findByMerchant_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                            merchantId,
                            startDateTime,
                            endDateTime,
                            pageable);
        }

        return paymentOrderRepository
                .findByMerchant_IdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        merchantId,
                        normalizedStatus,
                        startDateTime,
                        endDateTime,
                        pageable);
    }

    // Update payment order status
    public PaymentOrder updatePaymentOrderStatus(
            UUID id,
            String status) {

        PaymentOrder order = getPaymentOrderById(id);

        if (!"SUCCESS".equals(status)
                && !"FAILED".equals(status)) {
            throw new IllegalArgumentException(
                    "Status must be SUCCESS or FAILED");
        }

        if (!"CREATED".equals(order.getStatus())) {
            throw new IllegalStateException(
                    "Only CREATED orders can be updated");
        }

        order.setStatus(status);
        return paymentOrderRepository.save(order);
    }

    // Get detailed transaction summary for a merchant
    public Map<String, Object> getTransactionSummary(UUID merchantId) {

        long totalTransactions =
                paymentOrderRepository.countByMerchant_Id(merchantId);

        long successfulTransactions =
                paymentOrderRepository.countByMerchant_IdAndStatus(
                        merchantId, "SUCCESS");

        long failedTransactions =
                paymentOrderRepository.countByMerchant_IdAndStatus(
                        merchantId, "FAILED");

        long createdTransactions =
                paymentOrderRepository.countByMerchant_IdAndStatus(
                        merchantId, "CREATED");

        BigDecimal successfulRevenue =
                paymentOrderRepository.sumAmountByMerchantAndStatus(
                        merchantId, "SUCCESS");

        BigDecimal totalTransactionAmount =
                paymentOrderRepository.sumTotalAmountByMerchant(merchantId);

        BigDecimal averageTransactionAmount = BigDecimal.ZERO;
        BigDecimal successRate = BigDecimal.ZERO;
        BigDecimal failureRate = BigDecimal.ZERO;

        if (totalTransactions > 0) {

            BigDecimal total = BigDecimal.valueOf(totalTransactions);

            averageTransactionAmount = totalTransactionAmount.divide(
                    total,
                    2,
                    RoundingMode.HALF_UP
            );

            successRate = BigDecimal.valueOf(successfulTransactions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP);

            failureRate = BigDecimal.valueOf(failedTransactions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(total, 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("merchantId", merchantId);
        summary.put("totalTransactions", totalTransactions);
        summary.put("successfulTransactions", successfulTransactions);
        summary.put("failedTransactions", failedTransactions);
        summary.put("createdTransactions", createdTransactions);
        summary.put("successfulRevenue", successfulRevenue);
        summary.put("totalTransactionAmount", totalTransactionAmount);
        summary.put("averageTransactionAmount", averageTransactionAmount);
        summary.put("successRate", successRate);
        summary.put("failureRate", failureRate);

        return summary;
    }
}
