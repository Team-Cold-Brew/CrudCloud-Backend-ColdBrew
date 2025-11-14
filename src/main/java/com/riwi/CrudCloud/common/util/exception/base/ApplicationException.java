package com.riwi.CrudCloud.common.util.exception.base;

import lombok.Getter;

/**
 * Base exception for all application-specific errors.
 * 
 * All custom exceptions should extend this class (directly or indirectly)
 * to enable:
 * - Error code association
 * - i18n support
 * - Consistent error handling
 * - Type-based exception catching
 */
@Getter
public abstract class ApplicationException extends RuntimeException {
    private final int httpStatus;

    public ApplicationException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public ApplicationException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Get HTTP status code for this exception.
     * Override in subclasses to return appropriate status.
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
