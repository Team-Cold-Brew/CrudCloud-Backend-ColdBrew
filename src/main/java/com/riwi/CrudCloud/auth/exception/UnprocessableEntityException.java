package com.riwi.CrudCloud.auth.exception;

/**
 * Exception thrown when the request is syntactically valid but semantically incorrect.
 * HTTP Status: 422 Unprocessable Entity
 * 
 * Distinction from BadRequestException:
 * - 400 (BadRequest): Syntax is invalid (JSON doesn't parse, missing required fields)
 * - 422 (UnprocessableEntity): Syntax is valid, but violates business rules
 * 
 * Used for:
 * - Valid syntax, but contains restricted keywords/values
 * - Values are syntactically correct but violate business logic
 * - Semantic validation fails
 */
public class UnprocessableEntityException extends RuntimeException {
    
    public UnprocessableEntityException(String message) {
        super(message);
    }
    
    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
