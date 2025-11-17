package com.riwi.CrudCloud.common.util.exception;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.*;
import com.riwi.CrudCloud.common.util.exception.classes.server_errors.DatabaseManagementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.riwi.CrudCloud.common.util.exception.dto.ErrorResponse;
import com.riwi.CrudCloud.common.util.exception.handlers.client_errors.AuthenticationExceptionHandler;
import com.riwi.CrudCloud.common.util.exception.handlers.client_errors.BusinessLogicExceptionHandler;
import com.riwi.CrudCloud.common.util.exception.handlers.client_errors.OAuthExceptionHandler;
import com.riwi.CrudCloud.common.util.exception.handlers.client_errors.ResourceExceptionHandler;
import com.riwi.CrudCloud.common.util.exception.handlers.server_errors.SystemExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Global Exception Handler - Orchestrator Pattern
 * 
 * Central handler for all application exceptions. Acts as the single entry point
 * for exception handling, delegating to specialized handlers based on exception type.
 * 
 * Architecture:
 * ┌─────────────────────────────────────────┐
 * │  GlobalExceptionHandler (Orchestrator)  │
 * │  @RestControllerAdvice (ONLY ACTIVE)    │
 * └──────────────┬──────────────────────────┘
 *                │
 *     ┌──────────┼──────────┬──────────┬───────────┬──────────┐
 *     ▼          ▼          ▼          ▼           ▼          ▼
 *  Resource   Auth      Business    OAuth      System
 *  (404)      (401,403) (409,422)   (400,409)  (500)
 * 
 * Benefits:
 * ✅ Single entry point for all exceptions
 * ✅ Clear delegation logic
 * ✅ Easy to add/modify exception handling
 * ✅ Centralized logging and monitoring
 * ✅ No handler conflicts
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Autowired
    private ResourceExceptionHandler resourceHandler;

    @Autowired
    private AuthenticationExceptionHandler authHandler;

    @Autowired
    private BusinessLogicExceptionHandler businessLogicHandler;

    @Autowired
    private OAuthExceptionHandler oauthHandler;

    @Autowired
    private SystemExceptionHandler systemHandler;

    /**
     * Route ResourceNotFoundException to appropriate handler (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> routeResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Routing ResourceNotFoundException to ResourceExceptionHandler");
        return resourceHandler.handleResourceNotFoundException(ex);
    }

    /**
     * Route UnauthorizedException to appropriate handler (401 Unauthorized)
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> routeUnauthorized(UnauthorizedException ex) {
        log.debug("Routing UnauthorizedException to AuthenticationExceptionHandler");
        return authHandler.handleUnauthorizedException(ex);
    }

    /**
     * Route AuthException to appropriate handler (401 Unauthorized - Legacy)
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> routeAuthException(AuthException ex) {
        log.debug("Routing AuthException to AuthenticationExceptionHandler");
        return authHandler.handleAuthException(ex);
    }

    /**
     * Route ForbiddenException to appropriate handler (403 Forbidden)
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> routeForbidden(ForbiddenException ex) {
        log.debug("Routing ForbiddenException to AuthenticationExceptionHandler");
        return authHandler.handleForbiddenException(ex);
    }

    /**
     * Route ConflictException to appropriate handler (409 Conflict)
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> routeConflict(ConflictException ex) {
        log.debug("Routing ConflictException to BusinessLogicExceptionHandler");
        return businessLogicHandler.handleConflictException(ex);
    }

    /**
     * Route UnprocessableEntityException to appropriate handler (422 Unprocessable Entity)
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ErrorResponse> routeUnprocessableEntity(UnprocessableEntityException ex) {
        log.debug("Routing UnprocessableEntityException to BusinessLogicExceptionHandler");
        return businessLogicHandler.handleUnprocessableEntityException(ex);
    }

    /**
     * Route OAuthException to appropriate handler (400 Bad Request)
     */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ErrorResponse> routeOAuthException(OAuthException ex) {
        log.debug("Routing OAuthException to OAuthExceptionHandler");
        return oauthHandler.handleOAuthException(ex);
    }

    /**
     * Route AccountLinkingException to appropriate handler (409 Conflict)
     */
    @ExceptionHandler(AccountLinkingException.class)
    public ResponseEntity<ErrorResponse> routeAccountLinking(AccountLinkingException ex) {
        log.debug("Routing AccountLinkingException to OAuthExceptionHandler");
        return oauthHandler.handleAccountLinkingException(ex);
    }

    /**
     * Route generic exceptions to appropriate handler (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> routeGenericException(Exception ex) {
        log.error("Routing generic Exception to SystemExceptionHandler", ex);
        return systemHandler.handleGlobalException(ex);
    }

    /**
     * Route CustomNotFoundException to the appropriate handler (404 Not Found)
     */
    @ExceptionHandler(CustomNotFoundException.class)
    public ResponseEntity<ErrorResponse> routeCustomNotFound(CustomNotFoundException ex) {
        log.debug("Routing CustomNotFoundException (404) to ResourceExceptionHandler");
        // Creamos un ResourceNotFoundException con el mismo mensaje para delegar al handler especializado.
        return resourceHandler.handleResourceNotFoundException(new ResourceNotFoundException(ex.getMessage()));
    }

    /**
     * Route CustomBadRequestException to the appropriate handler (400 Bad Request)
     */
    @ExceptionHandler(CustomBadRequestException.class)
    public ResponseEntity<ErrorResponse> routeCustomBadRequest(CustomBadRequestException ex) {
        log.debug("Routing CustomBadRequestException (400) to BusinessLogicExceptionHandler");
        return businessLogicHandler.handleConflictException(new ConflictException(ex.getMessage()));

    }

    /**
     * Route DatabaseManagementException to SystemExceptionHandler (500 Internal Server Error)
     */
    @ExceptionHandler(DatabaseManagementException.class)
    public ResponseEntity<ErrorResponse> routeDatabaseManagementException(DatabaseManagementException ex) {
        log.error("Routing DatabaseManagementException (500) to SystemExceptionHandler", ex);
        return systemHandler.handleGlobalException(ex);
    }

}
