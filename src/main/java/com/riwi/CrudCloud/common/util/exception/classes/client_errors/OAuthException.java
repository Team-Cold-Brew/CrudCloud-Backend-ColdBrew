package com.riwi.CrudCloud.common.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.base.ClientErrorException;

/**
 * Exception for OAuth-related errors
 * HTTP Status: 400 Bad Request
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - OAuth provider communication failures
 * - Invalid OAuth tokens
 * - OAuth authorization failures
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class OAuthException extends ClientErrorException {
    private final String errorCode;

    public OAuthException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
        this.errorCode = "OAUTH_ERROR";
    }

    public OAuthException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST.value(), message, cause);
        this.errorCode = "OAUTH_ERROR";
    }

    public OAuthException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
        this.errorCode = errorCode;
    }

    public OAuthException(String errorCode, String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST.value(), message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return super.getMessage();
    }
}
