package com.riwi.CrudCloud.auth.util.exception.handlers.client_errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.ConflictException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.UnprocessableEntityException;
import com.riwi.CrudCloud.auth.util.exception.dto.ErrorResponse;

/**
 * Handles business logic errors for HTTP 409 Conflict and HTTP 422 Unprocessable Entity responses.
 * 
 * Manages:
 * - ConflictException (state machine violations, business rule conflicts)
 * - UnprocessableEntityException (valid syntax, but semantic/business rule violation)
 */
@RestControllerAdvice
public class BusinessLogicExceptionHandler {
    
    /**
     * Handle ConflictException - HTTP 409
     * Used when request conflicts with current resource state
     */
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle UnprocessableEntityException - HTTP 422
     * Used when request syntax is valid but violates business rules
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ErrorResponse> handleUnprocessableEntityException(
            UnprocessableEntityException ex) {
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
