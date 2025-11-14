package com.riwi.CrudCloud.auth.util.exception.classes.client_errors;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.auth.util.exception.base.ClientErrorException;

/**
 * Legacy authentication exception for backward compatibility.
 * HTTP Status: 401 Unauthorized
 * Category: ClientErrorException (4xx client error)
 * 
 * Note: Consider using UnauthorizedException for new code.
 * This class is maintained for backward compatibility with existing code.
 * 
 * Can be caught as: catch (ClientErrorException e) { ... }
 */
public class AuthException extends ClientErrorException {

    private String code;

    public AuthException(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
        this.code = "AUTH_ERROR";
    }

    public AuthException(String message, String code) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
