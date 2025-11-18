package com.riwi.CrudCloud.common.models;

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

/**
 * Payment entity
 * Represents a payment transaction in the system
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "mercadopago_payment_id", unique = true, length = 100)
    private String mercadopagoPaymentId;

    @Column(name = "preference_id", length = 100)
    private String preferenceId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "payer_email", length = 100)
    private String payerEmail;

    @Column(name = "payer_identification", length = 50)
    private String payerIdentification;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.currency == null) {
            this.currency = "ARS";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if payment is approved
     */
    public boolean isApproved() {
        return this.status == PaymentStatus.APPROVED;
    }

    /**
     * Check if payment is pending
     */
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING || this.status == PaymentStatus.IN_PROCESS;
    }

    /**
     * Check if payment failed
     */
    public boolean isFailed() {
        return this.status == PaymentStatus.REJECTED || this.status == PaymentStatus.CANCELLED;
    }
}
