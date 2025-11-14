package com.riwi.CrudCloud.common.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ClientErrorException;

/**
 * Exception thrown when a user lacks authorization to access a resource.
 * HTTP Status: 403 Forbidden
 * Category: ClientErrorException (4xx client error)
 * 
 * Key Distinction:
 * - 401 Unauthorized: "I don't know who you are" (authentication failure)
 * - 403 Forbidden: "I know who you are, but you can't do that" (authorization failure)
 * 
 * Used for:
 * - User tries to delete a resource they don't own
 * - User attempts to access restricted resources
 * - User lacks proper permissions for an operation
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
