
package com.payflow.repository;

import com.payflow.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByMerchantIdAndIdempotencyKey(
            UUID merchantId, String idempotencyKey
    );

    List<Refund> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Refund> findByPayment_IdAndMerchantIdOrderByCreatedAtDesc(
            UUID paymentId, UUID merchantId
    );

    List<Refund> findByPayment_Id(UUID paymentId);

    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);
}
