package com.riwi.CrudCloud.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.auth.dto.request.LoginRequest;
import com.riwi.CrudCloud.auth.dto.request.RegisterRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.UserResponse;
import com.riwi.CrudCloud.auth.service.AuthService;

import jakarta.validation.Valid;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register a new user
     * POST /api/auth/register
     *
     * @param registerRequest the registration request
     * @return ResponseEntity with AuthResponse
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     * POST /api/auth/login
     *
     * @param loginRequest the login request
     * @return ResponseEntity with AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user profile
     * GET /api/auth/profile
     *
     * @param userId the user ID (should come from JWT token in a real scenario)
     * @return ResponseEntity with UserResponse
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@RequestParam Integer userId) {
        UserResponse response = authService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }
}
