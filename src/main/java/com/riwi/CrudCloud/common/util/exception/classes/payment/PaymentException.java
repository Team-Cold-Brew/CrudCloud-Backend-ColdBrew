package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ServerErrorException;

/**
 * Base exception for payment-related server errors.
 * HTTP Status: 500 Internal Server Error
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - Payment processing failures
 * - Payment gateway errors
 * - Transaction processing errors
 * 
 * Can be caught as: catch (ServerErrorException e) { ... }
 */
public class PaymentException extends ServerErrorException {

    public PaymentException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public PaymentException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }
    
    public PaymentException(int httpStatus, String message) {
        super(httpStatus, message);
    }
    
    public PaymentException(int httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
