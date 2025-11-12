package com.riwi.CrudCloud.auth.util.exception.classes;

/**
 * Exception thrown when a user lacks authorization to access a resource.
 * HTTP Status: 403 Forbidden
 * 
 * Distinction:
 * - 401 Unauthorized: "I don't know who you are"
 * - 403 Forbidden: "I know who you are, but you can't do that"
 * 
 * Used for:
 * - User lacks permissions for an operation
 * - User attempts to access restricted resources
 * - Authorization checks fail
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
