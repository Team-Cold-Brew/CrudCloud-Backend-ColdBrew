package com.riwi.CrudCloud.auth.util.exception.classes;

/**
 * Exception thrown when a request conflicts with the current state of the resource.
 * HTTP Status: 409 Conflict
 * 
 * Used for:
 * - State machine violations
 * - Business rule conflicts
 * - Resource state prevents the operation
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
