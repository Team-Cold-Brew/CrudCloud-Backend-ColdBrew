package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;

import lombok.Getter;

/**
 * Exception thrown when a payment or transaction is not found.
 * HTTP Status: 404 Not Found
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Payment ID doesn't exist
 * - Transaction not found in database
 * - External payment reference not found
 * 
 * Can be caught as: catch (ResourceNotFoundException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class PaymentNotFoundException extends ResourceNotFoundException {

    private String transactionId;
    private String paymentId;

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public PaymentNotFoundException(String message, String transactionId, String paymentId) {
        super(message);
        this.transactionId = transactionId;
        this.paymentId = paymentId;
    }
    
    /**
     * Factory method for payment ID lookups
     */
    public static PaymentNotFoundException withPaymentId(String paymentId) {
        return new PaymentNotFoundException(
            "Payment not found with ID: " + paymentId,
            paymentId,
            paymentId
        );
    }
}
