package com.riwi.CrudCloud.common.util.exception.handlers.client_errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.AccountLinkingException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.OAuthException;
import com.riwi.CrudCloud.common.util.exception.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Handles OAuth-related exceptions.
 * Managed by GlobalExceptionHandler orchestrator.
 * 
 * Manages:
 * - OAuthException (HTTP 400 - OAuth provider failures)
 * - AccountLinkingException (HTTP 409 - Account linking conflicts)
 */
@Component
@Slf4j
public class OAuthExceptionHandler {

    /**
     * Handle OAuthException - HTTP 400
     * Used for OAuth provider communication failures
     */
    @ExceptionHandler(OAuthException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleOAuthException(
            OAuthException ex) {
        
        log.error("OAuth exception: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle AccountLinkingException - HTTP 409
     * Used for account linking conflicts
     */
    @ExceptionHandler(AccountLinkingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleAccountLinkingException(
            AccountLinkingException ex) {
        
        log.warn("Account linking exception: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}
