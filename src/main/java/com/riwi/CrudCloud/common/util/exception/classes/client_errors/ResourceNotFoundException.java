package com.riwi.CrudCloud.common.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ClientErrorException;

/**
 * Exception thrown when a requested resource cannot be found in the database.
 * HTTP Status: 404 Not Found
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Entity ID doesn't exist
 * - User requests a non-existent resource
 * - Database query returns no results
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class ResourceNotFoundException extends ClientErrorException {
    
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(HttpStatus.NOT_FOUND.value(), message, cause);
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
