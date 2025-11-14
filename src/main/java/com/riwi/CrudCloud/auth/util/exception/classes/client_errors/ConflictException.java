package com.riwi.CrudCloud.auth.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.base.ClientErrorException;

/**
 * Exception thrown when a request conflicts with the current state of the resource or violates uniqueness constraints.
 * HTTP Status: 409 Conflict
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - State machine violations (e.g., transitioning COMPLETED → PENDING)
 * - Duplicate email/username during registration
 * - Resource state prevents the operation
 * - Uniqueness constraint violations
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
