package com.riwi.CrudCloud.common.util.exception.classes.payment;

/**
 * Exception thrown when there are errors with MercadoPago API integration
 */
public class MercadoPagoException extends PaymentException {

    private String mercadoPagoErrorCode;
    private String mercadoPagoErrorMessage;

    public MercadoPagoException(String message) {
        super(message);
    }

    public MercadoPagoException(String message, Throwable cause) {
        super(message, cause);
    }

    public MercadoPagoException(String message, String mercadoPagoErrorCode, String mercadoPagoErrorMessage) {
        super(message);
        this.mercadoPagoErrorCode = mercadoPagoErrorCode;
        this.mercadoPagoErrorMessage = mercadoPagoErrorMessage;
    }

    public String getMercadoPagoErrorCode() {
        return mercadoPagoErrorCode;
    }

    public String getMercadoPagoErrorMessage() {
        return mercadoPagoErrorMessage;
    }
}
