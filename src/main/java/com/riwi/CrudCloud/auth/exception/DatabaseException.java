package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when database operations fail unexpectedly.
 * HTTP Status: 500 Internal Server Error
 * 
 * Used for:
 * - Database connection failures
 * - Unexpected database errors
 * - Transaction rollbacks
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
