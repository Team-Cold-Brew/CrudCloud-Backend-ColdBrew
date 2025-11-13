package com.riwi.CrudCloud.auth.util.exception.classes;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.ClientErrorException;

/**
 * Exception thrown when a request conflicts with the current state of the resource.
 * HTTP Status: 409 Conflict
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - State machine violations
 * - Business rule conflicts
 * - Resource state prevents the operation
 * - Duplicate resource creation (e.g., duplicate email)
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class ConflictException extends ClientErrorException {
    
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT.value(), message, cause);
    }
}
