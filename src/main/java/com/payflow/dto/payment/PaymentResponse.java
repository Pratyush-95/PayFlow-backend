
package com.payflow.dto.payment;

import com.payflow.entity.Payment;
import com.payflow.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String razorpayOrderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String description,
        LocalDateTime createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRazorpayOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getDescription(),
                payment.getCreatedAt()
        );
    }
}
