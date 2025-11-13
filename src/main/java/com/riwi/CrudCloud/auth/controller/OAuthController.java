package com.riwi.CrudCloud.auth.controller;

import com.riwi.CrudCloud.auth.dto.request.OAuthCallbackRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuthUserResponse;
import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.auth.service.GoogleOAuthService;
import com.riwi.CrudCloud.auth.service.GitHubOAuthService;
import com.riwi.CrudCloud.auth.service.OAuthUserProcessorService;
import com.riwi.CrudCloud.auth.dto.response.OAuth2TokenResponse;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.OAuthException;
import com.riwi.CrudCloud.auth.util.exception.classes.client_errors.AccountLinkingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for OAuth 2.0 authentication endpoints
 * Handles Google and GitHub OAuth callback flows
 */
@RestController
@RequestMapping("/api/auth/oauth")
@CrossOrigin(origins = "*")
@Slf4j
public class OAuthController {

    @Autowired
    private GoogleOAuthService googleOAuthService;

    @Autowired
    private GitHubOAuthService githubOAuthService;

    @Autowired
    private OAuthUserProcessorService oauthUserProcessorService;

    /**
     * Handle OAuth callback from provider
     * POST /api/auth/oauth/callback/{provider}
     *
     * @param provider the OAuth provider (google or github)
     * @param request the callback request containing code and state
     * @return ResponseEntity with AuthResponse containing JWT token
     */
    @PostMapping("/callback/{provider}")
    public ResponseEntity<?> handleOAuthCallback(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequest request) {

        log.info("OAuth callback received: provider={}", provider);

        try {
            OAuthProvider oauthProvider = OAuthProvider.fromValue(provider);

            // Step 1: Exchange authorization code for access token
            OAuth2TokenResponse tokenResponse = exchangeCodeForToken(request.getCode(), oauthProvider);

            // Step 2: Fetch user profile from OAuth provider
            OAuthUserResponse oauthUser = fetchUserProfile(tokenResponse.getAccessToken(), oauthProvider);

            // Step 3: Process user (create new or login existing)
            AuthResponse authResponse = oauthUserProcessorService.processOAuthUser(oauthUser, oauthProvider);

            log.info("OAuth authentication successful: email={}, provider={}", oauthUser.getEmail(), provider);
            return ResponseEntity.ok(authResponse);

        } catch (AccountLinkingException e) {
            log.warn("Account linking required: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "ACCOUNT_LINKING_REQUIRED",
                "message", e.getMessage(),
                "existingProvider", e.getExistingProvider()
            ));

        } catch (OAuthException e) {
            log.error("OAuth error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getErrorDescription()
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid OAuth provider: {}", provider);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_PROVIDER",
                "message", "Unsupported OAuth provider: " + provider
            ));

        } catch (Exception e) {
            log.error("Unexpected error during OAuth authentication", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred during authentication"
            ));
        }
    }

    /**
     * Link OAuth provider to existing user account
     * POST /api/auth/oauth/link/{provider}
     * Requires authentication
     *
     * @param provider the OAuth provider to link
     * @param request the callback request containing code and state
     * @return ResponseEntity with success message
     */
    @PostMapping("/link/{provider}")
    public ResponseEntity<?> linkOAuthProvider(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequest request) {

        log.info("OAuth provider linking requested: provider={}", provider);

        try {
            OAuthProvider oauthProvider = OAuthProvider.fromValue(provider);

            // Exchange authorization code for access token
            OAuth2TokenResponse tokenResponse = exchangeCodeForToken(request.getCode(), oauthProvider);

            // Fetch user profile from OAuth provider
            OAuthUserResponse oauthUser = fetchUserProfile(tokenResponse.getAccessToken(), oauthProvider);

            // For now, return success - frontend should handle the actual linking
            return ResponseEntity.ok(Map.of(
                "message", "OAuth provider ready for linking",
                "provider", provider,
                "email", oauthUser.getEmail()
            ));

        } catch (OAuthException e) {
            log.error("OAuth error during linking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", e.getErrorCode(),
                "message", e.getErrorDescription()
            ));

        } catch (Exception e) {
            log.error("Unexpected error during OAuth provider linking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred"
            ));
        }
    }

    /**
     * Exchange authorization code for access token
     */
    private OAuth2TokenResponse exchangeCodeForToken(String code, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleOAuthService.exchangeCodeForToken(code);
            case GITHUB -> githubOAuthService.exchangeCodeForToken(code);
            default -> throw new OAuthException("INVALID_PROVIDER", "Unsupported OAuth provider: " + provider);
        };
    }

    /**
     * Fetch user profile from OAuth provider
     */
    private OAuthUserResponse fetchUserProfile(String accessToken, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleOAuthService.getUserProfile(accessToken);
            case GITHUB -> githubOAuthService.getUserProfile(accessToken);
            default -> throw new OAuthException("INVALID_PROVIDER", "Unsupported OAuth provider: " + provider);
        };
    }
}
