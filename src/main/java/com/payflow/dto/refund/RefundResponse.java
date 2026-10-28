
package com.payflow.dto.refund;

import com.payflow.entity.Refund;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        String razorpayRefundId,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getRazorpayRefundId(),
                refund.getAmount(),
                refund.getPayment().getCurrency(),
                refund.getStatus().name(),
                refund.getReason(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}
