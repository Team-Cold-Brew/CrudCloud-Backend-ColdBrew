package com.riwi.CrudCloud.common.util.exception.handlers.server_errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.riwi.CrudCloud.common.util.exception.classes.server_errors.DatabaseException;
import com.riwi.CrudCloud.common.util.exception.dto.ErrorResponse;

/**
 * Handles system-level errors for HTTP 500 Internal Server Error responses.
 * Managed by GlobalExceptionHandler orchestrator.
 * 
 * Manages:
 * - DatabaseException (database operation failures)
 * - Exception (generic fallback for unexpected errors)
 */
@Component
public class SystemExceptionHandler {
    
    /**
     * Handle DatabaseException
     */
    @ExceptionHandler(DatabaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Generic fallback handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
