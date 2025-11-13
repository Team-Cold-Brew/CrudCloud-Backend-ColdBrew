package com.riwi.CrudCloud.auth.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.base.ClientErrorException;

/**
 * Exception for account linking scenarios
 * HTTP Status: 409 Conflict
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Account already linked to different provider
 * - Account linking conflicts
 * - Multiple provider scenarios
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class AccountLinkingException extends ClientErrorException {
    private String existingProvider;

    public AccountLinkingException(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }

    public AccountLinkingException(String message, String existingProvider) {
        super(HttpStatus.CONFLICT.value(), message);
        this.existingProvider = existingProvider;
    }

    public AccountLinkingException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT.value(), message, cause);
    }

    public String getExistingProvider() {
        return existingProvider;
    }

    public void setExistingProvider(String existingProvider) {
        this.existingProvider = existingProvider;
    }
}
