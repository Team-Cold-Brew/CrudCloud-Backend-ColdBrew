package com.riwi.CrudCloud.mercadoPago.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Preference entity
 * Represents a MercadoPago Checkout Pro preference
 */
@Entity
@Table(name = "payment_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    @Column(name = "mercadopago_preference_id", unique = true, nullable = false, length = 100)
    private String mercadopagoPreferenceId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency_id", nullable = false, length = 3)
    private String currencyId;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "init_point", length = 500)
    private String initPoint;

    @Column(name = "sandbox_init_point", length = 500)
    private String sandboxInitPoint;

    @Column(name = "is_expired", nullable = false)
    private Boolean isExpired;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isExpired == null) {
            this.isExpired = false;
        }
        if (this.quantity == null) {
            this.quantity = 1;
        }
        if (this.currencyId == null) {
            this.currencyId = "ARS";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if preference is expired
     */
    public boolean hasExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }
}
