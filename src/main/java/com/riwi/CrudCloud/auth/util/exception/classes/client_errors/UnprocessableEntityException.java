package com.riwi.CrudCloud.auth.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.base.ClientErrorException;

/**
 * Exception thrown when the request is syntactically valid but semantically incorrect.
 * HTTP Status: 422 Unprocessable Entity
 * Category: ClientErrorException (4xx client error)
 * 
 * Key Distinction from BadRequestException:
 * - 400 (BadRequest): Syntax is invalid (JSON doesn't parse, missing required fields)
 * - 422 (UnprocessableEntity): Syntax is valid, but violates business rules or semantic validation
 * 
 * Used for:
 * - Request JSON is valid, but contains restricted keywords
 * - Values are syntactically correct but violate business logic
 * - Semantic validation fails
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class UnprocessableEntityException extends ClientErrorException {
    
    public UnprocessableEntityException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), message);
    }
    
    public UnprocessableEntityException(String message, Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY.value(), message, cause);
    }
}
