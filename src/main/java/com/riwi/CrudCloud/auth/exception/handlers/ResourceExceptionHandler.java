package com.riwi.CrudCloud.auth.exception.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.riwi.CrudCloud.auth.exception.ResourceNotFoundException;
import com.riwi.CrudCloud.auth.exception.dto.ErrorResponse;

/**
 * Handles resource not found errors for HTTP 404 Not Found responses.
 * 
 * Manages:
 * - ResourceNotFoundException (entity not found scenarios)
 */
@RestControllerAdvice
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
