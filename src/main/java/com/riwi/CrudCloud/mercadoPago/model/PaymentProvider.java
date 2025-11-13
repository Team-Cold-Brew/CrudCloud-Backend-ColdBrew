package com.riwi.CrudCloud.mercadoPago.model;

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

@Entity
@Table(name = "payment_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Integer providerId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate the payment provider
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivate the payment provider
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Check if provider is active
     */
    public boolean isActive() {
        return this.active != null && this.active;
    }
}
