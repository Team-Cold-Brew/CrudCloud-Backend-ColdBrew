package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when subscription operations fail.
 * HTTP Status: 500 Internal Server Error
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - Failed to create subscription
 * - Failed to activate subscription
 * - Failed to cancel subscription
 * - Subscription status update errors
 * 
 * Can be caught as: catch (PaymentException e) { ... } or catch (ServerErrorException e) { ... }
 */
@Getter
public class SubscriptionException extends PaymentException {

    private String userId;
    private String planId;
    private String subscriptionId;

    public SubscriptionException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public SubscriptionException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }

    public SubscriptionException(String message, String userId, String planId) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.userId = userId;
        this.planId = planId;
    }
    
    public SubscriptionException(String message, String userId, String planId, String subscriptionId) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.userId = userId;
        this.planId = planId;
        this.subscriptionId = subscriptionId;
    }
}
