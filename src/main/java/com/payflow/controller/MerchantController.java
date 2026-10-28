
package com.payflow.controller;

import com.payflow.entity.Merchant;
import com.payflow.entity.PaymentOrder;
import com.payflow.service.MerchantService;
import com.payflow.service.PaymentOrderService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final PaymentOrderService paymentOrderService;

    public MerchantController(
            MerchantService merchantService,
            PaymentOrderService paymentOrderService) {

        this.merchantService = merchantService;
        this.paymentOrderService = paymentOrderService;
    }

    // Create a merchant
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Merchant createMerchant(
            @RequestBody MerchantRequest request) {

        return merchantService.createMerchant(
                request.getName(),
                request.getEmail()
        );
    }

    // Get all merchants
    @GetMapping
    public List<Merchant> getAllMerchants() {
        return merchantService.getAllMerchants();
    }

    // Get merchant by ID
    @GetMapping("/{id}")
    public Merchant getMerchantById(@PathVariable UUID id) {
        return merchantService.getMerchantById(id);
    }

    // Get merchant payment orders with filters, sorting and pagination
    @GetMapping("/{id}/payment-orders")
    public Page<PaymentOrder> getMerchantPaymentOrders(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        // Verify merchant exists
        merchantService.getMerchantById(id);

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        if (!List.of(
                "createdAt", "amount", "status", "currency"
        ).contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy field");
        }

        if (!"asc".equalsIgnoreCase(direction)
                && !"desc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException(
                    "Direction must be asc or desc");
        }

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return paymentOrderService.getPaymentOrdersByDateRange(
                id,
                period,
                startDate,
                endDate,
                status,
                pageable
        );
    }

    // Get transaction summary for a merchant
    @GetMapping("/{id}/payment-orders/summary")
    public Map<String, Object> getTransactionSummary(
            @PathVariable UUID id) {

        // Verify merchant exists
        merchantService.getMerchantById(id);

        return paymentOrderService.getTransactionSummary(id);
    }

    // Update a merchant
    @PutMapping("/{id}")
    public Merchant updateMerchant(
            @PathVariable UUID id,
            @RequestBody MerchantRequest request) {

        return merchantService.updateMerchant(
                id,
                request.getName(),
                request.getEmail()
        );
    }

    // Delete a merchant
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMerchant(
            @PathVariable UUID id) {

        merchantService.deleteMerchant(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Merchant deleted successfully"
                )
        );
    }

    // Request DTO
    public static class MerchantRequest {

        private String name;
        private String email;

        public MerchantRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
