package com.riwi.CrudCloud.common.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ClientErrorException;

/**
 * Exception thrown when a request contains syntactically invalid data.
 * HTTP Status: 400 Bad Request
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Missing required fields
 * - Invalid data types
 * - Malformed request syntax
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class BadRequestException extends ClientErrorException {
    
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST.value(), message, cause);
    }
}
