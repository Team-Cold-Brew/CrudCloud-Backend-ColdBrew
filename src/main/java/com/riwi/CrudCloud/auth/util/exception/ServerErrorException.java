package com.riwi.CrudCloud.auth.util.exception;

/**
 * Base exception for all 5xx server errors.
 * 
 * Use this class to catch all server-caused errors:
 *   catch (ServerErrorException e) { ... }
 * 
 * Examples: 500, 502, 503, 504
 * Meaning: Server encountered an internal problem
 * Action: Retry with exponential backoff
 */
public class ServerErrorException extends ApplicationException {
    public ServerErrorException(int httpStatus, String message) {
        super(httpStatus, message);
    }

    public ServerErrorException(int httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
