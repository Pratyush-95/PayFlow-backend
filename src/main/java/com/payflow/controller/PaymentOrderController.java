
package com.payflow.controller;

import com.payflow.CreatePaymentOrderRequest;
import com.payflow.PaymentOrderCreationResult;
import com.payflow.entity.PaymentOrder;
import com.payflow.service.PaymentOrderService;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    public PaymentOrderController(
            PaymentOrderService paymentOrderService) {
        this.paymentOrderService = paymentOrderService;
    }

    // POST: Create a payment order with idempotency
    @PostMapping
    public ResponseEntity<PaymentOrder> createPaymentOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentOrderRequest request) {

        String currency = request.getCurrency().toUpperCase();

        PaymentOrder order = new PaymentOrder(
                request.getAmount(),
                currency
        );

        PaymentOrderCreationResult result =
                paymentOrderService.createPaymentOrder(
                        order,
                        idempotencyKey,
                        request.getMerchantId()
                );

        if (result.created()) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(result.order());
        }

        return ResponseEntity.ok(result.order());
    }

    // GET: Fetch payment order by ID
    @GetMapping("/{id}")
    public PaymentOrder getPaymentOrderById(
            @PathVariable UUID id) {
        return paymentOrderService.getPaymentOrderById(id);
    }

    // GET: Fetch all payment orders
    @GetMapping
    public List<PaymentOrder> getAllPaymentOrders() {
        return paymentOrderService.getAllPaymentOrders();
    }

    // PATCH: Update payment order status
    @PatchMapping("/{id}/status")
    public PaymentOrder updatePaymentOrderStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        return paymentOrderService.updatePaymentOrderStatus(
                id, status
        );
    }
}
