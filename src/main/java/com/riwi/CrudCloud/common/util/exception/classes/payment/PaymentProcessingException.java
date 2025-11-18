package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when there are errors processing payments.
 * HTTP Status: 500 Internal Server Error
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - Payment processing failures
 * - Transaction verification errors
 * - Payment status update failures
 * 
 * Can be caught as: catch (PaymentException e) { ... } or catch (ServerErrorException e) { ... }
 */
@Getter
public class PaymentProcessingException extends PaymentException {

    private String transactionId;
    private String paymentStatus;

    public PaymentProcessingException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }

    public PaymentProcessingException(String message, String transactionId, String paymentStatus) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
        this.transactionId = transactionId;
        this.paymentStatus = paymentStatus;
    }
}
