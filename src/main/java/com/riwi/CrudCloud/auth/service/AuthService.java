package com.riwi.CrudCloud.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.auth.dto.request.LoginRequest;
import com.riwi.CrudCloud.auth.dto.request.OAuth2LoginRequest;
import com.riwi.CrudCloud.auth.dto.request.RegisterRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.UserResponse;
import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.models.UserStatus;
import com.riwi.CrudCloud.common.models.UserType;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.auth.util.TokenService;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.ConflictException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.ResourceNotFoundException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.UnauthorizedException;

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
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(userResponse)
            .message("User registered successfully")
            .build();
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
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(userResponse)
            .message("Login successful")
            .build();
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
     * Login or register user via OAuth
     * 
     * If user already exists with OAuth provider, login.
     * If user doesn't exist, create new user and register.
     *
     * @param oauthRequest the OAuth login request
     * @param providerUserId the user ID from OAuth provider
     * @param providerEmail the email from OAuth provider
     * @param providerName the name from OAuth provider
     * @param profilePictureUrl the profile picture URL from OAuth provider
     * @return AuthResponse with token and user details
     * @throws UnauthorizedException if OAuth validation fails
     */
    @Transactional
    public AuthResponse oauthLogin(OAuth2LoginRequest oauthRequest, String providerUserId, 
                                   String providerEmail, String providerName, String profilePictureUrl) {
        OAuthProvider provider = OAuthProvider.valueOf(oauthRequest.getProvider().toUpperCase());
        
        // Check if user already exists with this OAuth provider
        User existingUser = findUserByOAuthProvider(provider, providerUserId);
        
        if (existingUser != null) {
            // User already registered with OAuth, login them
            String token = tokenService.generateToken(existingUser);
            UserResponse userResponse = mapToUserResponse(existingUser);
            return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userResponse)
                .message("OAuth login successful")
                .build();
        }
        
        // Check if email already exists
        String username = generateUsernameFromEmail(providerEmail);
        if (userRepository.findByEmail(providerEmail).isPresent()) {
            throw new ConflictException("Email already exists: " + providerEmail);
        }
        
        // Create new user with OAuth provider
        User newUser = User.builder()
            .email(providerEmail)
            .username(username)
            .firstName(extractFirstName(providerName))
            .lastName(extractLastName(providerName))
            .profilePictureUrl(profilePictureUrl)
            .userType(UserType.valueOf(oauthRequest.getUserType().toUpperCase()))
            .status(UserStatus.ACTIVE)
            .oauthProvider(provider)
            .build();
        
        // Set OAuth-specific ID based on provider
        if (provider == OAuthProvider.GOOGLE) {
            newUser.setGoogleId(providerUserId);
        } else if (provider == OAuthProvider.GITHUB) {
            newUser.setGithubId(providerUserId);
        }
        
        User savedUser = userRepository.save(newUser);
        
        // Generate JWT token
        String token = tokenService.generateToken(savedUser);
        
        UserResponse userResponse = mapToUserResponse(savedUser);
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(userResponse)
            .message("OAuth registration successful")
            .build();
    }

    /**
     * Find user by OAuth provider
     */
    @Transactional(readOnly = true)
    private User findUserByOAuthProvider(OAuthProvider provider, String providerUserId) {
        if (provider == OAuthProvider.GOOGLE) {
            return userRepository.findByGoogleId(providerUserId).orElse(null);
        } else if (provider == OAuthProvider.GITHUB) {
            return userRepository.findByGithubId(providerUserId).orElse(null);
        }
        return null;
    }

    /**
     * Generate username from email
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsernameAndNotDeleted(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }

    /**
     * Extract first name from full name
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    /**
     * Extract last name from full name
     */
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return null;
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return null;
    }

    /**
     * Map User entity to UserResponse DTO
     * Note: Password is never included in responses
     *
     * @param user the user entity
     * @return UserResponse
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .profilePictureUrl(user.getProfilePictureUrl())
            .userType(user.getUserType())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
