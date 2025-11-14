package com.riwi.CrudCloud.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OAuth2LoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider; // GOOGLE or GITHUB

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @NotNull(message = "User type is required")
    private String userType;

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
