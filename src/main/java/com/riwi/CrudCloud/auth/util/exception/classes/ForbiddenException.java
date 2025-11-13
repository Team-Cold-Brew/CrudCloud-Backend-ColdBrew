package com.riwi.CrudCloud.auth.util.exception.classes;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.ClientErrorException;

/**
 * Exception thrown when a user lacks authorization to access a resource.
 * HTTP Status: 403 Forbidden
 * Category: ClientErrorException (4xx client error)
 * 
 * Distinction:
 * - 401 Unauthorized: "I don't know who you are"
 * - 403 Forbidden: "I know who you are, but you can't do that"
 * 
 * Used for:
 * - User lacks permissions for an operation
 * - User attempts to access restricted resources
 * - Authorization checks fail
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class ForbiddenException extends ClientErrorException {
    
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN.value(), message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN.value(), message, cause);
    }
}
