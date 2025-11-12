package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when user provides invalid login credentials.
 * Extends BadRequestException (HTTP 400 Bad Request)
 * 
 * Used for:
 * - Invalid email/password combinations
 * - Authentication failures
 * 
 * Note: In a more strict implementation, this could use HTTP 401 Unauthorized,
 * but BadRequest is used here for failed validation of credentials.
 * 
 * @deprecated Consider throwing BadRequestException directly with context
 */
@Deprecated(since = "1.1", forRemoval = true)
public class InvalidCredentialsException extends BadRequestException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
