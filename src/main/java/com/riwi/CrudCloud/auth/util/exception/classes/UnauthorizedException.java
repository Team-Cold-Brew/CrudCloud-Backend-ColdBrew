package com.riwi.CrudCloud.auth.util.exception.classes;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.ClientErrorException;

/**
 * Exception thrown when authentication fails or credentials are invalid.
 * HTTP Status: 401 Unauthorized
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Invalid email/password combination
 * - Expired or invalid JWT token
 * - Missing authentication credentials
 * - Authentication validation failures
 * 
 * Distinction:
 * - 401 Unauthorized: "I don't know who you are" or "Invalid credentials"
 * - 403 Forbidden: "I know who you are, but you can't do that"
 * - 400 Bad Request: "Your request format is invalid"
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
