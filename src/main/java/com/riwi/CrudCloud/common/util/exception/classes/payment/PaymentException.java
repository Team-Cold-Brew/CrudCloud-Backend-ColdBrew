package com.riwi.CrudCloud.common.util.exception.classes.payment;

/**
 * Base exception for payment-related errors
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
