package com.riwi.CrudCloud.auth.dto.response;

import java.time.LocalDateTime;

import com.riwi.CrudCloud.common.models.UserType;

public class UserResponse {

    private Integer userId;
    private String username;
    private String email;
    private UserType userType;
    private String status;
    private LocalDateTime createdAt;

    // Constructor
    public UserResponse(Integer userId, String username, String email, UserType userType, 
                       String status, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
