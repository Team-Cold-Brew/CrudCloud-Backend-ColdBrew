package com.riwi.CrudCloud.common.util.exception.handlers.client_errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.AuthException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ForbiddenException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.UnauthorizedException;
import com.riwi.CrudCloud.common.util.exception.dto.ErrorResponse;

/**
 * Handles authentication and authorization errors.
 * Managed by GlobalExceptionHandler orchestrator.
 * 
 * Manages:
 * - UnauthorizedException (HTTP 401 - Invalid credentials/authentication failures)
 * - ForbiddenException (HTTP 403 - User lacks permissions)
 * 
 * Key Distinction:
 * - 401 Unauthorized: "I don't know who you are" OR "Invalid credentials"
 *   Examples: Wrong password, expired token, missing credentials
 * 
 * - 403 Forbidden: "I know who you are, but you can't do that"
 *   Examples: User lacks permission to delete resource, insufficient role
 */
@Component
public class AuthenticationExceptionHandler {
    
    /**
     * Handle UnauthorizedException (HTTP 401)
     * Used for authentication failures: invalid credentials, expired tokens, etc.
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle AuthException (HTTP 401 - Legacy)
     * Used for legacy authentication failures for backward compatibility
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthException(
            AuthException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle ForbiddenException (HTTP 403)
     * Used for authorization failures: user lacks permissions
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
}
