package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;

import lombok.Getter;

/**
 * Exception thrown when a payment preference is not found.
 * HTTP Status: 404 Not Found
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Preference ID doesn't exist
 * - External preference reference not found
 * - Preference not found in database
 * 
 * Can be caught as: catch (ResourceNotFoundException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class PreferenceNotFoundException extends ResourceNotFoundException {

    private String preferenceId;
    private String externalId;

    public PreferenceNotFoundException(String message) {
        super(message);
    }

    public PreferenceNotFoundException(String message, String preferenceId) {
        super(message);
        this.preferenceId = preferenceId;
    }
    
    public PreferenceNotFoundException(String message, String preferenceId, String externalId) {
        super(message);
        this.preferenceId = preferenceId;
        this.externalId = externalId;
    }
    
    /**
     * Factory method for preference ID lookups
     */
    public static PreferenceNotFoundException withPreferenceId(String preferenceId) {
        return new PreferenceNotFoundException(
            "Payment preference not found with ID: " + preferenceId,
            preferenceId
        );
    }
    
    /**
     * Factory method for external ID lookups
     */
    public static PreferenceNotFoundException withExternalId(String externalId) {
        return new PreferenceNotFoundException(
            "Payment preference not found with external ID: " + externalId,
            null,
            externalId
        );
    }
}
