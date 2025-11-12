package com.riwi.CrudCloud.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.auth.dto.request.LoginRequest;
import com.riwi.CrudCloud.auth.dto.request.RegisterRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.UserResponse;
import com.riwi.CrudCloud.auth.exception.ConflictException;
import com.riwi.CrudCloud.auth.exception.ResourceNotFoundException;
import com.riwi.CrudCloud.auth.exception.UnauthorizedException;
import com.riwi.CrudCloud.auth.model.User;
import com.riwi.CrudCloud.auth.model.UserStatus;
import com.riwi.CrudCloud.auth.repository.UserRepository;

/**
 * Service class for user authentication and user management
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Register a new user
     *
     * @param registerRequest the registration request
     * @return AuthResponse with token and user details
     * @throws ConflictException if email or username already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmailAndNotDeleted(registerRequest.getEmail())) {
            throw new ConflictException("Email already exists: " + registerRequest.getEmail());
        }

        // Check if username already exists
        if (userRepository.existsByUsernameAndNotDeleted(registerRequest.getUsername())) {
            throw new ConflictException("Username already exists: " + registerRequest.getUsername());
        }

        // Create new user
        User user = User.builder()
            .email(registerRequest.getEmail())
            .username(registerRequest.getUsername())
            .password(registerRequest.getPassword()) // TODO: Hash the password with BCrypt
            .userType(registerRequest.getUserType())
            .status(UserStatus.ACTIVE)
            .build();

        User savedUser = userRepository.save(user);

        // TODO: Generate JWT token
        String token = "temp-token-placeholder";

        UserResponse userResponse = mapToUserResponse(savedUser);
        return new AuthResponse(token, userResponse);
    }

    /**
     * Login user with email and password
     *
     * @param loginRequest the login request
     * @return AuthResponse with token and user details
     * @throws ResourceNotFoundException if user not found (404)
     * @throws UnauthorizedException if password is invalid (401)
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + loginRequest.getEmail()));

        // TODO: Verify password with BCrypt
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // TODO: Generate JWT token
        String token = "temp-token-placeholder";

        UserResponse userResponse = mapToUserResponse(user);
        return new AuthResponse(token, userResponse);
    }

    /**
     * Get user profile by ID
     *
     * @param userId the user ID
     * @return UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return mapToUserResponse(user);
    }

    /**
     * Validate token (placeholder for JWT validation)
     *
     * @param token the JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        // TODO: Implement JWT validation
        return true;
    }

    /**
     * Extract user ID from token (placeholder for JWT extraction)
     *
     * @param token the JWT token
     * @return user ID
     */
    public Integer extractUserIdFromToken(String token) {
        // TODO: Implement JWT extraction
        return null;
    }

    /**
     * Map User entity to UserResponse DTO
     *
     * @param user the user entity
     * @return UserResponse
     */
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getUserType(),
            user.getStatus().toString(),
            user.getCreatedAt()
        );
    }
}
