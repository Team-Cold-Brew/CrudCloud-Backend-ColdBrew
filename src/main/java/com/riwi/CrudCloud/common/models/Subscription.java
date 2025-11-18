package com.riwi.CrudCloud.common.models;

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
 * Subscription entity
 * Represents a user's subscription to a plan
 */
@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(name = "auto_renewal", nullable = false)
    private Boolean autoRenewal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SubscriptionStatus.PENDING;
        }
        if (this.autoRenewal == null) {
            this.autoRenewal = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if subscription is active
     */
    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE && 
               (this.endDate == null || this.endDate.isAfter(LocalDateTime.now()));
    }

    /**
     * Check if subscription is expired
     */
    public boolean isExpired() {
        return this.endDate != null && this.endDate.isBefore(LocalDateTime.now());
    }

    /**
     * Cancel subscription
     */
    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.autoRenewal = false;
    }

    /**
     * Activate subscription
     */
    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = LocalDateTime.now();
        
        // Set end date based on plan billing cycle
        if (plan != null && "monthly".equalsIgnoreCase(plan.getBillingCycle())) {
            this.endDate = LocalDateTime.now().plusMonths(1);
            this.nextBillingDate = this.endDate;
        } else if (plan != null && "yearly".equalsIgnoreCase(plan.getBillingCycle())) {
            this.endDate = LocalDateTime.now().plusYears(1);
            this.nextBillingDate = this.endDate;
        }
    }
}
