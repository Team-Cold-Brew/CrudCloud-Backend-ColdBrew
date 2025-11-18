package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when checkout preference creation fails.
 * HTTP Status: 500 Internal Server Error
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - Failed to create MercadoPago preference
 * - Invalid checkout configuration
 * - Preference service unavailable
 * 
 * Can be caught as: catch (PaymentException e) { ... } or catch (ServerErrorException e) { ... }
 */
@Getter
public class CheckoutCreationException extends PaymentException {

    private String userId;
    private String planId;
    private String externalReference;

    public CheckoutCreationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public CheckoutCreationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }

    public CheckoutCreationException(String message, String userId, String planId) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.userId = userId;
        this.planId = planId;
    }
    
    public CheckoutCreationException(String message, String userId, String planId, String externalReference) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.userId = userId;
        this.planId = planId;
        this.externalReference = externalReference;
    }
}
