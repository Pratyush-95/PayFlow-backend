
package com.payflow.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final String razorpayKeySecret;

    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret
    ) throws Exception {

        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.razorpayKeySecret = keySecret;
    }

    // Create Razorpay order
    public String createOrder(
            BigDecimal amount,
            String currency,
            UUID paymentId
    ) throws Exception {

        long amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", paymentId.toString());

        Order order = razorpayClient.orders.create(orderRequest);

        return order.get("id");
    }

    // Verify Razorpay payment signature
    public boolean verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) throws Exception {

        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        return Utils.verifyPaymentSignature(
                attributes,
                razorpayKeySecret
        );
    }

    // Fetch payment and verify captured status and details
    public boolean isPaymentCaptured(
            String paymentId,
            String expectedOrderId,
            long expectedAmountInPaise,
            String expectedCurrency
    ) throws Exception {

        com.razorpay.Payment payment =
                razorpayClient.payments.fetch(paymentId);

        Object actualId = payment.get("id");
        Object actualOrderId = payment.get("order_id");
        Object actualStatus = payment.get("status");
        Object actualAmount = payment.get("amount");
        Object actualCurrency = payment.get("currency");

        return paymentId.equals(actualId)
                && expectedOrderId.equals(actualOrderId)
                && "captured".equals(actualStatus)
                && actualAmount instanceof Number
                && ((Number) actualAmount).longValue()
                == expectedAmountInPaise
                && expectedCurrency.equals(actualCurrency);
    }

    // Create a full or partial refund through Razorpay
    public com.razorpay.Refund createRefund(
            String razorpayPaymentId,
            long amountInPaise,
            String internalRefundId
    ) throws Exception {

        if (razorpayPaymentId == null
                || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is required"
            );
        }

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero"
            );
        }

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", amountInPaise);
        refundRequest.put("receipt", internalRefundId);

        JSONObject notes = new JSONObject();
        notes.put("internal_refund_id", internalRefundId);
        refundRequest.put("notes", notes);

        return razorpayClient.payments.refund(
                razorpayPaymentId,
                refundRequest
        );
    }
}
