package com.riwi.CrudCloud.common.util.exception.classes.server_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ServerErrorException;

/**
 * Exception thrown when database operations fail unexpectedly.
 * HTTP Status: 500 Internal Server Error
 * Category: ServerErrorException (5xx server error)
 * 
 * Used for:
 * - Database connection failures
 * - Unexpected database errors
 * - Transaction rollbacks
 * - Query timeouts
 * - Connection pool exhausted
 * 
 * This is a transient error - can be retried with backoff.
 * 
 * Can be caught as: catch (ServerErrorException e) { ... }
 */
public class DatabaseException extends ServerErrorException {
    
    public DatabaseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }
}
