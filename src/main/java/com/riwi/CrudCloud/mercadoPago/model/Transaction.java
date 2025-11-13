package com.riwi.CrudCloud.mercadoPago.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.riwi.CrudCloud.auth.model.User;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "organization_id")
    private Integer organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "provider_transaction_id", nullable = false, unique = true, length = 300)
    private String providerTransactionId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private TransactionStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as approved
     */
    public void approve() {
        this.status = TransactionStatus.APPROVED;
        this.approvalDate = LocalDateTime.now();
    }

    /**
     * Mark transaction as rejected
     */
    public void reject() {
        this.status = TransactionStatus.REJECTED;
    }

    /**
     * Mark transaction as refunded
     */
    public void refund() {
        this.status = TransactionStatus.REFUNDED;
    }

    /**
     * Check if transaction is approved
     */
    public boolean isApproved() {
        return this.status == TransactionStatus.APPROVED;
    }

    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }
}
