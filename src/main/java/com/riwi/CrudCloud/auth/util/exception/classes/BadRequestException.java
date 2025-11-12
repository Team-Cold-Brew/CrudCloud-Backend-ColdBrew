package com.riwi.CrudCloud.auth.util.exception.classes;

/**
 * Exception thrown when a request contains syntactically invalid data.
 * HTTP Status: 400 Bad Request
 * 
 * Used for:
 * - Missing required fields
 * - Invalid data types
 * - Malformed request syntax
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
