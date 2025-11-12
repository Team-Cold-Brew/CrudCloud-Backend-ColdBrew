package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Extends ConflictException (HTTP 409 Conflict)
 * 
 * Used for:
 * - Duplicate email registration attempts
 * - Duplicate username registration attempts
 * - Any resource that violates unique constraints
 * 
 * @deprecated Consider throwing ConflictException directly instead
 */
@Deprecated(since = "1.1", forRemoval = true)
public class UserAlreadyExistsException extends ConflictException {

    public UserAlreadyExistsException(String field, String value) {
        super(field + " already exists: " + value);
    }
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
