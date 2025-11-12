package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when authentication fails or credentials are invalid.
 * HTTP Status: 401 Unauthorized
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
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
