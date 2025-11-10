package com.riwi.CrudCloud.auth.exception;

public class UserAlreadyExistsException extends AuthException {

    public UserAlreadyExistsException(String field, String value) {
        super(field + " already exists: " + value, "USER_ALREADY_EXISTS");
    }
}
