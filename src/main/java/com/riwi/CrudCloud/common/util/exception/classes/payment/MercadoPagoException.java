package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Exception thrown when there are errors with MercadoPago API integration.
 * HTTP Status: 502 Bad Gateway
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - MercadoPago API errors
 * - SDK integration failures
 * - Payment gateway communication errors
 * 
 * Can be caught as: catch (PaymentException e) { ... } or catch (ServerErrorException e) { ... }
 */
@Getter
public class MercadoPagoException extends PaymentException {

    private String mercadoPagoErrorCode;
    private String mercadoPagoErrorMessage;
    private Integer mercadoPagoStatusCode;

    public MercadoPagoException(String message) {
        super(HttpStatus.BAD_GATEWAY.value(), message);
    }

    public MercadoPagoException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY.value(), message, cause);
    }

    public MercadoPagoException(String message, String mercadoPagoErrorCode, String mercadoPagoErrorMessage) {
        super(HttpStatus.BAD_GATEWAY.value(), message);
        this.mercadoPagoErrorCode = mercadoPagoErrorCode;
        this.mercadoPagoErrorMessage = mercadoPagoErrorMessage;
    }
    
    public MercadoPagoException(String message, String mercadoPagoErrorCode, String mercadoPagoErrorMessage, Integer statusCode) {
        super(HttpStatus.BAD_GATEWAY.value(), message);
        this.mercadoPagoErrorCode = mercadoPagoErrorCode;
        this.mercadoPagoErrorMessage = mercadoPagoErrorMessage;
        this.mercadoPagoStatusCode = statusCode;
    }
}
