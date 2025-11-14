package com.riwi.CrudCloud.common.util.exception.base;

/**
 * Base exception for all 4xx client errors.
 * 
 * Use this class to catch all client-caused errors:
 *   catch (ClientErrorException e) { ... }
 * 
 * Examples: 400, 401, 403, 404, 409, 422
 * Meaning: Client made an error in their request or state
 * Action: Don't retry - fix the request first
 */
public class ClientErrorException extends ApplicationException {
    public ClientErrorException(int httpStatus, String message) {
        super(httpStatus, message);
    }

    public ClientErrorException(int httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
