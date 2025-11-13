package com.riwi.CrudCloud.auth.service;

import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuthUserResponse;
import com.riwi.CrudCloud.auth.dto.response.UserResponse;
import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.models.UserStatus;
import com.riwi.CrudCloud.common.models.UserType;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.auth.util.TokenService;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.AccountLinkingException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.OAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

/**
 * Service for processing OAuth user authentication
 * Handles user creation, account linking, and data extraction
 */
@Service
@Slf4j
public class OAuthUserProcessorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Process OAuth user and return authentication response
     * Handles three scenarios:
     * 1. New OAuth user - creates new account
     * 2. Existing OAuth user - returns existing account
     * 3. Email conflict - throws exception for account linking
     *
     * @param oAuthUser OAuth user data from provider
     * @param provider OAuth provider (GOOGLE or GITHUB)
     * @return AuthResponse with token and user details
     */
    @Transactional
    public AuthResponse processOAuthUser(OAuthUserResponse oAuthUser, OAuthProvider provider) {
        log.info("Processing OAuth user: email={}, provider={}", oAuthUser.getEmail(), provider);

        // Step 1: Check if user exists with same OAuth provider ID
        Optional<User> existingUserByProvider = findUserByProvider(oAuthUser.getProviderId(), provider);
        if (existingUserByProvider.isPresent()) {
            log.debug("User already exists with provider ID: email={}, provider={}", 
                oAuthUser.getEmail(), provider);
            User user = existingUserByProvider.get();
            return buildAuthResponse(user);
        }

        // Step 2: Check if user exists by email
        Optional<User> existingUserByEmail = userRepository.findByEmailAndNotDeleted(oAuthUser.getEmail());
        if (existingUserByEmail.isPresent()) {
            User existingUser = existingUserByEmail.get();
            
            // Check what authentication method is linked to this account
            OAuthProvider existingProvider = determineExistingProvider(existingUser);
            log.warn("Account linking required: email={}, newProvider={}, existingProvider={}", 
                oAuthUser.getEmail(), provider, existingProvider);
            
            // Throw exception with existing provider info
            throw new AccountLinkingException(
                "Email already associated with an existing account",
                existingProvider.getValue()
            );
        }

        // Step 3: Create new user with OAuth data
        User newUser = createUserFromOAuth(oAuthUser, provider);
        User savedUser = userRepository.save(newUser);
        log.info("New OAuth user created: userId={}, email={}, provider={}", 
            savedUser.getUserId(), savedUser.getEmail(), provider);

        return buildAuthResponse(savedUser);
    }

    /**
     * Link OAuth provider to existing user account
     * Called when user confirms account linking
     *
     * @param userId the user ID
     * @param oAuthUser OAuth user data
     * @param provider OAuth provider to link
     * @return AuthResponse with token and user details
     */
    @Transactional
    public AuthResponse linkOAuthProvider(Integer userId, OAuthUserResponse oAuthUser, OAuthProvider provider) {
        log.info("Linking OAuth provider to user: userId={}, provider={}", userId, provider);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new OAuthException("USER_NOT_FOUND", "User not found: " + userId));

        // Link the OAuth provider to the user
        linkProviderToUser(user, oAuthUser, provider);
        
        User updatedUser = userRepository.save(user);
        log.info("OAuth provider linked successfully: userId={}, provider={}", userId, provider);

        return buildAuthResponse(updatedUser);
    }

    /**
     * Find user by OAuth provider ID
     */
    private Optional<User> findUserByProvider(String providerId, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> userRepository.findByGoogleIdAndNotDeleted(providerId);
            case GITHUB -> userRepository.findByGithubIdAndNotDeleted(providerId);
            default -> Optional.empty();
        };
    }

    /**
     * Determine which provider is linked to existing user
     */
    private OAuthProvider determineExistingProvider(User user) {
        if (user.getGoogleId() != null && !user.getGoogleId().isEmpty()) {
            return OAuthProvider.GOOGLE;
        }
        if (user.getGithubId() != null && !user.getGithubId().isEmpty()) {
            return OAuthProvider.GITHUB;
        }
        return OAuthProvider.MANUAL;
    }

    /**
     * Create new user from OAuth data
     */
    private User createUserFromOAuth(OAuthUserResponse oAuthUser, OAuthProvider provider) {
        String username = generateUsername(oAuthUser.getEmail());

        User user = User.builder()
            .username(username)
            .email(oAuthUser.getEmail())
            .password(passwordEncoder.encode(generateRandomPassword())) // OAuth users don't need passwords
            .firstName(oAuthUser.getFirstName())
            .lastName(oAuthUser.getLastName())
            .profilePictureUrl(oAuthUser.getProfilePictureUrl())
            .userType(UserType.CUSTOMER) // Default user type
            .status(UserStatus.ACTIVE)
            .oauthProvider(provider)
            .build();

        // Set provider-specific ID
        linkProviderToUser(user, oAuthUser, provider);

        return user;
    }

    /**
     * Link OAuth provider credentials to user
     */
    private void linkProviderToUser(User user, OAuthUserResponse oAuthUser, OAuthProvider provider) {
        switch (provider) {
            case GOOGLE -> user.setGoogleId(oAuthUser.getProviderId());
            case GITHUB -> user.setGithubId(oAuthUser.getProviderId());
            default -> {
                // Do nothing for MANUAL
            }
        }
    }

    /**
     * Generate unique username from email
     */
    private String generateUsername(String email) {
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
     * Generate random password for OAuth users
     * OAuth users don't use passwords for login
     */
    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 32; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }

        return password.toString();
    }

    /**
     * Build AuthResponse from User
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = tokenService.generateToken(user.getUserId());
        UserResponse userResponse = UserResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .userType(user.getUserType())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .profilePictureUrl(user.getProfilePictureUrl())
            .status(user.getStatus())
            .build();

        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(userResponse)
            .message("OAuth authentication successful")
            .build();
    }
}
