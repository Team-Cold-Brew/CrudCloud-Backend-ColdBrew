package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when a requested resource cannot be found in the database.
 * HTTP Status: 404 Not Found
 * 
 * Used for:
 * - Entity ID doesn't exist
 * - User requests a non-existent resource
 * - Database query returns no results
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Factory method for easier resource not found messages
     */
    public static ResourceNotFoundException withResource(String resourceType, String field, Object value) {
        return new ResourceNotFoundException(
            String.format("%s not found with %s: %s", resourceType, field, value)
        );
    }
    
    /**
     * Factory method for ID-based lookups
     */
    public static ResourceNotFoundException withId(String resourceType, Object id) {
        return new ResourceNotFoundException(
            String.format("%s not found with ID: %s", resourceType, id)
        );
    }
}
