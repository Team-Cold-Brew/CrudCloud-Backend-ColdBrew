package com.riwi.CrudCloud.auth.exception;

public class UserNotFoundException extends AuthException {

    public UserNotFoundException(String field, String value) {
        super("User not found with " + field + ": " + value, "USER_NOT_FOUND");
    }

    public UserNotFoundException(Integer userId) {
        super("User not found with ID: " + userId, "USER_NOT_FOUND");
    }
}
