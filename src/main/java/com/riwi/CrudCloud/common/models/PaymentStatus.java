package com.riwi.CrudCloud.common.models;

/**
 * Payment status enum
 * Represents the different states a payment can have in the system
 */
public enum PaymentStatus {
    /**
     * Payment is pending approval
     */
    PENDING,

    /**
     * Payment has been approved
     */
    APPROVED,

    /**
     * Payment was rejected
     */
    REJECTED,

    /**
     * Payment was cancelled
     */
    CANCELLED,

    /**
     * Payment has been refunded
     */
    REFUNDED,

    /**
     * Payment is in process
     */
    IN_PROCESS,

    /**
     * Payment is in mediation
     */
    IN_MEDIATION,

    /**
     * Payment was charged back
     */
    CHARGED_BACK
}
