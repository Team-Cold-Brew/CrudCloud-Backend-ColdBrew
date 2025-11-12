package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when a user cannot be found in the database.
 * Extends ResourceNotFoundException (HTTP 404 Not Found)
 * 
 * Used for:
 * - User ID lookup failures
 * - User email lookup failures
 * - User doesn't exist scenarios
 * 
 * @deprecated Consider throwing ResourceNotFoundException directly instead
 */
@Deprecated(since = "1.1", forRemoval = true)
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String field, String value) {
        super("User not found with " + field + ": " + value);
    }

    public UserNotFoundException(Integer userId) {
        super("User not found with ID: " + userId);
    }
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
