package com.riwi.CrudCloud.auth.util.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;

import com.riwi.CrudCloud.auth.util.exception.handlers.client_errors.AuthenticationExceptionHandler;
import com.riwi.CrudCloud.auth.util.exception.handlers.client_errors.BusinessLogicExceptionHandler;
import com.riwi.CrudCloud.auth.util.exception.handlers.client_errors.OAuthExceptionHandler;
import com.riwi.CrudCloud.auth.util.exception.handlers.client_errors.ResourceExceptionHandler;
import com.riwi.CrudCloud.auth.util.exception.handlers.server_errors.SystemExceptionHandler;
import com.riwi.CrudCloud.auth.util.exception.handlers.client_errors.ValidationExceptionHandler;

/**
 * Global exception handler orchestrator for the auth module.
 * 
 * Acts as the central registration point for all specialized exception handlers.
 * Each handler manages a specific category of exceptions with appropriate HTTP status codes.
 * 
 * Architecture Overview:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │        GlobalExceptionHandler (Orchestrator)                    │
 * │            (@ControllerAdvice)                                 │
 * └─────────────────────────────────────────────────────────────────┘
 *              │
 *    ┌─────────┼──────────┬──────────┬───────────┬──────────┬─────────┐
 *    ▼         ▼          ▼          ▼           ▼          ▼         ▼
 * Validation  Resource  Auth       Business   System     OAuth
 * (400)       (404)     (401,403)  (409,422)  (500)      (400,409)
 * 
 * Handler Categories:
 * - ValidationExceptionHandler: HTTP 400 Bad Request
 * - ResourceExceptionHandler: HTTP 404 Not Found
 * - AuthenticationExceptionHandler: HTTP 401 Unauthorized & 403 Forbidden
 * - BusinessLogicExceptionHandler: HTTP 409 Conflict & 422 Unprocessable Entity
 * - SystemExceptionHandler: HTTP 500 Internal Server Error
 * - OAuthExceptionHandler: HTTP 400 Bad Request (OAuth) & 409 Conflict (Account Linking)
 * 
 * Benefits:
 * ✅ Separation of Concerns: Each handler manages one category
 * ✅ Maintainability: Easy to locate and modify specific exception handling
 * ✅ Scalability: New handlers can be added without modifying existing code
 * ✅ Single Responsibility: Each handler focuses on one task
 * ✅ Reusability: Handler patterns can be copied for new exceptions
 */
@ControllerAdvice(
    basePackageClasses = {
        ValidationExceptionHandler.class,
        ResourceExceptionHandler.class,
        AuthenticationExceptionHandler.class,
        BusinessLogicExceptionHandler.class,
        SystemExceptionHandler.class,
        OAuthExceptionHandler.class
    }
)
public class GlobalExceptionHandler {
    // Orchestrator - delegates to specialized handlers
}
