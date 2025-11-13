package com.riwi.CrudCloud.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.auth.dto.request.LoginRequest;
import com.riwi.CrudCloud.auth.dto.request.RegisterRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.UserResponse;
import com.riwi.CrudCloud.auth.model.User;
import com.riwi.CrudCloud.auth.model.UserStatus;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.auth.util.TokenService;
import com.riwi.CrudCloud.auth.util.exception.classes.ConflictException;
import com.riwi.CrudCloud.auth.util.exception.classes.ResourceNotFoundException;
import com.riwi.CrudCloud.auth.util.exception.classes.UnauthorizedException;

/**
 * Service class for user authentication and user management
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

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

        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());

        // Create new user
        User user = User.builder()
            .email(registerRequest.getEmail())
            .username(registerRequest.getUsername())
            .password(hashedPassword)
            .userType(registerRequest.getUserType())
            .status(UserStatus.ACTIVE)
            .build();

        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = tokenService.generateToken(savedUser);

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

        // Verify password with BCrypt
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate JWT token
        String token = tokenService.generateToken(user);

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
     * Validate token using TokenService
     *
     * @param token the JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        return tokenService.validateToken(token);
    }

    /**
     * Extract user ID from token using TokenService
     *
     * @param token the JWT token
     * @return user ID
     */
    public Integer extractUserIdFromToken(String token) {
        return tokenService.extractUserIdFromToken(token);
    }

    /**
     * Map User entity to UserResponse DTO
     * Note: Password is never included in responses
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
