package com.riwi.CrudCloud.mercadoPago.util.exception.classes;

/**
 * Exception thrown when payment data is invalid or incomplete
 */
public class InvalidPaymentDataException extends PaymentException {

    private String fieldName;
    private Object fieldValue;

    public InvalidPaymentDataException(String message) {
        super(message);
    }

    public InvalidPaymentDataException(String message, String fieldName, Object fieldValue) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
