package com.riwi.CrudCloud.auth.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.base.ClientErrorException;

/**
 * Exception thrown when authentication fails (invalid credentials).
 * HTTP Status: 401 Unauthorized
 * Category: ClientErrorException (4xx client error)
 * 
 * Key Distinction:
 * - 401 Unauthorized: "I don't know who you are" or "Invalid credentials"
 * - 403 Forbidden: "I know who you are, but you can't do that"
 * 
 * Used for:
 * - Wrong password provided
 * - Invalid authentication token
 * - User not found during login
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class UnauthorizedException extends ClientErrorException {
    
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED.value(), message, cause);
    }
}
