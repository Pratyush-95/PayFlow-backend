
package com.payflow;

import com.payflow.entity.PaymentOrder;

public record PaymentOrderCreationResult(
        PaymentOrder order,
        boolean created
) {
}
