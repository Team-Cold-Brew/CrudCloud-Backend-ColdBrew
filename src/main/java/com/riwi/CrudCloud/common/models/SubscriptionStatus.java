package com.riwi.CrudCloud.common.models;

/**
 * Subscription status enum
 * Represents the different states a subscription can have
 */
public enum SubscriptionStatus {
    /**
     * Subscription is pending activation
     */
    PENDING,

    /**
     * Subscription is active
     */
    ACTIVE,

    /**
     * Subscription has been cancelled
     */
    CANCELLED,

    /**
     * Subscription has expired
     */
    EXPIRED,

    /**
     * Subscription is suspended
     */
    SUSPENDED
}
