
package com.payflow.repository;

import com.payflow.entity.Payment;
import com.payflow.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Get all payments for a merchant
    Page<Payment> findByMerchantIdOrderByCreatedAtDesc(
            UUID merchantId,
            Pageable pageable
    );

    // Get payments filtered by status
    Page<Payment> findByMerchantIdAndStatusOrderByCreatedAtDesc(
            UUID merchantId,
            PaymentStatus status,
            Pageable pageable
    );

    // Find payment using Razorpay order ID
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Count all payments for a merchant
    long countByMerchantId(UUID merchantId);

    // Count payments by status
    long countByMerchantIdAndStatus(
            UUID merchantId,
            PaymentStatus status
    );

    // Sum amount of all payments for a merchant
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.merchantId = :merchantId
        """)
    BigDecimal sumAmountByMerchantId(
            @Param("merchantId") UUID merchantId
    );

    // Sum amount by payment status
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.merchantId = :merchantId
          AND p.status = :status
        """)
    BigDecimal sumAmountByMerchantIdAndStatus(
            @Param("merchantId") UUID merchantId,
            @Param("status") PaymentStatus status
    );

    // Lock payment by internal UUID during refund processing
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findWithLockById(
            @Param("id") UUID id
    );

    // Lock payment by Razorpay order ID during verification/webhooks
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :orderId")
    Optional<Payment> findWithLockByRazorpayOrderId(
            @Param("orderId") String orderId
    );
}
