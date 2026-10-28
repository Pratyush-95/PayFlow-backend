
package com.payflow.repository;

import com.payflow.entity.PaymentOrder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository
        extends JpaRepository<PaymentOrder, UUID> {

    // Find payment order using idempotency key
    Optional<PaymentOrder> findByIdempotencyKey(
            String idempotencyKey
    );

    // Fetch paginated payment orders for a merchant
    Page<PaymentOrder> findByMerchant_Id(
            UUID merchantId,
            Pageable pageable
    );

    // Fetch paginated payment orders by merchant and status
    Page<PaymentOrder> findByMerchant_IdAndStatus(
            UUID merchantId,
            String status,
            Pageable pageable
    );

    // Fetch orders within a date range
    Page<PaymentOrder>
    findByMerchant_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID merchantId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    );

    // Fetch orders by merchant, status, and date range
    Page<PaymentOrder>
    findByMerchant_IdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID merchantId,
            String status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    );

    // Fetch all merchant transactions in a date range for PDF reports
    List<PaymentOrder>
    findByMerchant_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID merchantId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    // Count all orders belonging to a merchant
    long countByMerchant_Id(UUID merchantId);

    // Count orders by merchant and status
    long countByMerchant_IdAndStatus(
            UUID merchantId,
            String status
    );

    // Calculate successful payment revenue
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PaymentOrder p
            WHERE p.merchant.id = :merchantId
              AND p.status = :status
            """)
    BigDecimal sumAmountByMerchantAndStatus(
            @Param("merchantId") UUID merchantId,
            @Param("status") String status
    );

    // Calculate total amount across all merchant transactions
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PaymentOrder p
            WHERE p.merchant.id = :merchantId
            """)
    BigDecimal sumTotalAmountByMerchant(
            @Param("merchantId") UUID merchantId
    );
}
