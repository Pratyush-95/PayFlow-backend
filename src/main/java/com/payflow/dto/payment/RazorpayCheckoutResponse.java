
package com.payflow.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record RazorpayCheckoutResponse(
        UUID paymentId,
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        String currency,
        String status,
        String description
) {
}
