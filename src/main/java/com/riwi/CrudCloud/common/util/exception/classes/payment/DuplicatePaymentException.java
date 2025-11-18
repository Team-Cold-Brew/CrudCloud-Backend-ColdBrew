package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ConflictException;

import lombok.Getter;

/**
 * Exception thrown when attempting to process a payment that already exists.
 * HTTP Status: 409 Conflict
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Payment already processed
 * - Duplicate webhook notification
 * - Payment ID already exists in database
 * 
 * Can be caught as: catch (ConflictException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class DuplicatePaymentException extends ConflictException {

    private String paymentId;
    private String externalReference;

    public DuplicatePaymentException(String message) {
        super(message);
    }

    public DuplicatePaymentException(String message, String paymentId) {
        super(message);
        this.paymentId = paymentId;
    }
    
    public DuplicatePaymentException(String message, String paymentId, String externalReference) {
        super(message);
        this.paymentId = paymentId;
        this.externalReference = externalReference;
    }
    
    /**
     * Factory method for duplicate payment detection
     */
    public static DuplicatePaymentException alreadyProcessed(String paymentId) {
        return new DuplicatePaymentException(
            "Payment already processed with ID: " + paymentId,
            paymentId
        );
    }
}
