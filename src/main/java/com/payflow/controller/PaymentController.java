
package com.payflow.controller;
import java.math.BigDecimal;

import com.payflow.dto.payment.PaymentRequest;
import com.payflow.dto.payment.PaymentResponse;
import com.payflow.dto.payment.RazorpayCheckoutResponse;
import com.payflow.dto.payment.RazorpayVerifyRequest;
import com.payflow.service.PaymentService;
import com.payflow.entity.PaymentStatus;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create payment and Razorpay order
    @PostMapping
    public ResponseEntity<?> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication
    ) {
        UUID merchantId;

        try {
            merchantId = getMerchantId(authentication);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        try {
            RazorpayCheckoutResponse response =
                    paymentService.createPayment(merchantId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "message",
                            "Unable to create payment order"
                    ));
        }
    }

    // Verify Razorpay payment
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody RazorpayVerifyRequest request,
            Authentication authentication
    ) {
        UUID merchantId;

        try {
            merchantId = getMerchantId(authentication);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        try {
            PaymentResponse response =
                    paymentService.verifyPayment(merchantId, request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", ex.getMessage()));
        }
    }



    // Get authenticated merchant's payment history with optional status filter
    @GetMapping
    public ResponseEntity<?> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PaymentStatus status
    ) {
        UUID merchantId;

        try {
            merchantId = getMerchantId(authentication);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        try {
            return ResponseEntity.ok(
                    paymentService.getPaymentHistory(
                            merchantId, page, size, status
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getPaymentSummary(
            Authentication authentication
    ) {
        UUID merchantId;

        try {
            merchantId = getMerchantId(authentication);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        return ResponseEntity.ok(
                paymentService.getPaymentSummary(merchantId)
        );
    }


    // Get a payment belonging to the authenticated merchant
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID merchantId;

        try {
            merchantId = getMerchantId(authentication);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }

        try {
            return ResponseEntity.ok(
                    paymentService.getPaymentById(merchantId, id)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    private UUID getMerchantId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Authentication is required"
            );
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid authenticated user ID"
            );
        }
    }
}
