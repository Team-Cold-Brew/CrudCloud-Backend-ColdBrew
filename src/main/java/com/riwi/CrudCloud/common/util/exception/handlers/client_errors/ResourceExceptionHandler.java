package com.riwi.CrudCloud.common.util.exception.handlers.client_errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;
import com.riwi.CrudCloud.common.util.exception.dto.ErrorResponse;

/**
 * Handles resource not found errors for HTTP 404 Not Found responses.
 * Managed by GlobalExceptionHandler orchestrator.
 * 
 * Manages:
 * - ResourceNotFoundException (entity not found scenarios)
 */
@Component
public class ResourceExceptionHandler {
    
    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
