package com.riwi.CrudCloud.auth.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.auth.dto.request.OAuthCallbackRequest;
import com.riwi.CrudCloud.auth.dto.response.AuthResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuth2TokenResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuthUserResponse;
import com.riwi.CrudCloud.auth.service.GitHubOAuthService;
import com.riwi.CrudCloud.auth.service.GoogleOAuthService;
import com.riwi.CrudCloud.auth.service.OAuthUserProcessorService;
import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.AccountLinkingException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.OAuthException;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for OAuth 2.0 authentication endpoints
 * Handles Google and GitHub OAuth callback flows
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
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
     * Initiate OAuth flow - returns authorization URL
     * GET /api/v1/auth/oauth/authorize/{provider}
     *
     * @param provider the OAuth provider (google or github)
     * @param redirectUri the redirect URI for OAuth callback
     * @return ResponseEntity with authorization URL to redirect to
     */
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<?> authorizeOAuth(
            @PathVariable String provider,
            @RequestParam String redirectUri) {

        log.info("OAuth authorization initiated: provider={}", provider);

        try {
            OAuthProvider oauthProvider = OAuthProvider.fromValue(provider);

            String authorizationUrl = switch (oauthProvider) {
                case GOOGLE -> googleOAuthService.getAuthorizationUrl(redirectUri);
                case GITHUB -> githubOAuthService.getAuthorizationUrl(redirectUri);
                default -> throw new OAuthException("INVALID_PROVIDER", "Unsupported OAuth provider: " + provider);
            };

            return ResponseEntity.ok(Map.of(
                "authorizationUrl", authorizationUrl
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid OAuth provider: {}", provider);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_PROVIDER",
                "message", "Unsupported OAuth provider: " + provider
            ));
        } catch (Exception e) {
            log.error("Error generating authorization URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "AUTHORIZATION_URL_GENERATION_FAILED",
                "message", "Failed to generate authorization URL"
            ));
        }
    }

    /**
     * Handle OAuth callback from provider (Browser redirect with query params)
     * GET /api/v1/auth/oauth/callback/{provider}?code=...&state=...
     * 
     * This endpoint receives the browser redirect from OAuth providers
     * and exchanges the code for an access token
     *
     * @param provider the OAuth provider (google or github)
     * @param code the authorization code from OAuth provider
     * @param state the state parameter for CSRF protection
     * @return ResponseEntity with AuthResponse containing JWT token
     */
    @GetMapping("/callback/{provider}")
    public ResponseEntity<?> handleOAuthCallbackGet(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("OAuth callback received (GET): provider={}, code={}, state={}", provider, code, state);

        try {
            OAuthProvider oauthProvider = OAuthProvider.fromValue(provider);

            // Step 1: Exchange authorization code for access token
            OAuth2TokenResponse tokenResponse = exchangeCodeForToken(code, oauthProvider);

            // Step 2: Fetch user profile from OAuth provider
            OAuthUserResponse oauthUser = fetchUserProfile(tokenResponse.getAccessToken(), oauthProvider);

            // Step 3: Process user (create new or login existing)
            AuthResponse authResponse = oauthUserProcessorService.processOAuthUser(oauthUser, oauthProvider);

            log.info("OAuth authentication successful: email={}, provider={}", oauthUser.getEmail(), provider);
            
            // Return the token in the response - frontend will handle storage
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
     * Handle OAuth callback from frontend (POST with JSON body)
     * POST /api/v1/auth/oauth/callback
     * 
     * This endpoint receives OAuth callback from frontend after Google redirects
     * Provider is determined from state parameter
     *
     * @param request the callback request containing code and state
     * @return ResponseEntity with AuthResponse containing JWT token
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleOAuthCallbackPost(
            @Valid @RequestBody OAuthCallbackRequest request) {

        log.info("OAuth callback received (POST): state={}", request.getState());

        try {
            // Extract provider from state (format: "provider_timestamp")
            String provider = "google"; // Default to google
            if (request.getState() != null && request.getState().contains("_")) {
                provider = request.getState().split("_")[0];
            }

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
            log.error("Invalid OAuth provider from state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_PROVIDER",
                "message", "Could not determine OAuth provider from state parameter"
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
