package com.riwi.CrudCloud.auth.dto.response;

public class AuthResponse {

    private String token;
    private UserResponse user;

    // Constructor
    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
