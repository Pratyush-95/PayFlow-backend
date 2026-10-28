
package com.payflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refunds",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_merchant_idempotency",
                        columnNames = {"merchant_id", "idempotency_key"}
                ),
                @UniqueConstraint(
                        name = "uk_refund_razorpay_id",
                        columnNames = {"razorpay_refund_id"}
                )
        },
        indexes = {
                @Index(name = "idx_refund_payment", columnList = "payment_id"),
                @Index(name = "idx_refund_merchant", columnList = "merchant_id"),
                @Index(name = "idx_refund_status", columnList = "status")
        }
)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "razorpay_refund_id", unique = true, length = 100)
    private String razorpayRefundId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Refund() {}

    public Refund(Payment payment, UUID merchantId, String idempotencyKey,
                  BigDecimal amount, String reason) {
        this.payment = payment;
        this.merchantId = merchantId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Payment getPayment() { return payment; }
    public UUID getMerchantId() { return merchantId; }
    public String getRazorpayRefundId() { return razorpayRefundId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public RefundStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setRazorpayRefundId(String id) { this.razorpayRefundId = id; }
    public void setStatus(RefundStatus status) { this.status = status; }
}
