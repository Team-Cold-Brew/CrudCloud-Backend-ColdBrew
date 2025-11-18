package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.BadRequestException;

import lombok.Getter;

/**
 * Exception thrown when payment data is invalid or incomplete.
 * HTTP Status: 400 Bad Request
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Invalid payment amount
 * - Missing required payment fields
 * - Invalid payment method data
 * - Malformed payment request
 * 
 * Can be caught as: catch (BadRequestException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class InvalidPaymentDataException extends BadRequestException {

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
    
    /**
     * Factory method for field validation errors
     */
    public static InvalidPaymentDataException withField(String fieldName, Object fieldValue, String reason) {
        return new InvalidPaymentDataException(
            String.format("Invalid payment data for field '%s': %s", fieldName, reason),
            fieldName,
            fieldValue
        );
    }
}
