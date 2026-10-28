
package com.payflow.controller;

import com.payflow.dto.refund.RefundRequest;
import com.payflow.dto.refund.RefundResponse;
import com.payflow.service.RefundService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/{paymentId}/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<?> createRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody com.payflow.dto.refund.RefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            Authentication authentication) {

        try {
            UUID merchantId = getMerchantId(authentication);

            RefundResponse response = refundService.createRefund(
                    merchantId, paymentId, request, idempotencyKey);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to process refund request"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getRefunds(
            @PathVariable UUID paymentId,
            Authentication authentication) {

        try {
            UUID merchantId = getMerchantId(authentication);

            return ResponseEntity.ok(
                    refundService.getRefunds(merchantId, paymentId));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    private UUID getMerchantId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authentication is required");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid authenticated user ID");
        }
    }
}
