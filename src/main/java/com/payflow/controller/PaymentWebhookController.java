
package com.payflow.controller;

import com.payflow.service.PaymentService;
import com.payflow.service.RefundService;
import com.razorpay.Utils;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final String webhookSecret;

    public PaymentWebhookController(
            PaymentService paymentService,
            RefundService refundService,
            @Value("${razorpay.webhook.secret}") String webhookSecret
    ) {
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            ) String signature
    ) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Missing webhook signature"));
        }

        try {
            // Verify signature against the original, unmodified body.
            boolean valid = Utils.verifyWebhookSignature(
                    rawBody,
                    signature,
                    webhookSecret
            );

            if (!valid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid webhook signature"));
            }

            JSONObject payload = new JSONObject(rawBody);
            String event = payload.optString("event", "");

            // Handle captured and failed payment events.
            if ("payment.captured".equals(event)
                    || "payment.failed".equals(event)) {

                JSONObject paymentEntity = payload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String paymentId = paymentEntity.optString("id", "");
                String orderId = paymentEntity.optString("order_id", "");

                if (paymentId.isBlank() || orderId.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "message",
                                    "Missing payment ID or order ID"
                            ));
                }

                if ("payment.captured".equals(event)) {
                    paymentService.handleCapturedWebhook(
                            orderId,
                            paymentId
                    );
                } else {
                    String reason = paymentEntity.optString(
                            "error_description",
                            "Payment failed"
                    );

                    paymentService.handleFailedWebhook(
                            orderId,
                            paymentId,
                            reason
                    );
                }

                return ResponseEntity.ok(Map.of("received", true));
            }

            // Handle refund lifecycle events.
            if ("refund.processed".equals(event)
                    || "refund.failed".equals(event)) {

                JSONObject refundEntity = payload
                        .getJSONObject("payload")
                        .getJSONObject("refund")
                        .getJSONObject("entity");

                String refundId = refundEntity.optString("id", "");
                String status = refundEntity.optString("status", "");

                if (refundId.isBlank() || status.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "message",
                                    "Missing refund ID or status"
                            ));
                }

                // Service updates only the matching internal refund record.
                refundService.updateRefundStatus(refundId, status);

                return ResponseEntity.ok(Map.of("received", true));
            }

            // Acknowledge other valid, unsupported events.
            return ResponseEntity.ok(Map.of("received", true));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message",
                            "Webhook payment or refund data is invalid"
                    ));
        } catch (Exception exception) {
            // A server error lets the provider retry transient failures.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message",
                            "Webhook could not be processed"
                    ));
        }
    }
}
