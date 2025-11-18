package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;

import lombok.Getter;

/**
 * Exception thrown when a subscription is not found.
 * HTTP Status: 404 Not Found
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Subscription ID doesn't exist
 * - User has no active subscription
 * - Subscription not found in database
 * 
 * Can be caught as: catch (ResourceNotFoundException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class SubscriptionNotFoundException extends ResourceNotFoundException {

    private String userId;
    private String subscriptionId;

    public SubscriptionNotFoundException(String message) {
        super(message);
    }

    public SubscriptionNotFoundException(String message, String userId) {
        super(message);
        this.userId = userId;
    }
    
    public SubscriptionNotFoundException(String message, String userId, String subscriptionId) {
        super(message);
        this.userId = userId;
        this.subscriptionId = subscriptionId;
    }
    
    /**
     * Factory method for subscription ID lookups
     */
    public static SubscriptionNotFoundException withSubscriptionId(String subscriptionId) {
        return new SubscriptionNotFoundException(
            "Subscription not found with ID: " + subscriptionId,
            null,
            subscriptionId
        );
    }
    
    /**
     * Factory method for user subscription lookups
     */
    public static SubscriptionNotFoundException forUser(String userId) {
        return new SubscriptionNotFoundException(
            "No active subscription found for user: " + userId,
            userId
        );
    }
}
