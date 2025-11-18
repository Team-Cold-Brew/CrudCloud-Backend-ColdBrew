package com.riwi.CrudCloud.auth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.auth.dto.response.OAuthProviderResponse;
import com.riwi.CrudCloud.auth.service.OAuthProviderService;
import com.riwi.CrudCloud.common.models.OAuthProvider;

/**
 * Controller for OAuth provider management endpoints
 */
@RestController
@RequestMapping("/api/v1/auth/oauth/providers")
@CrossOrigin(origins = "*")
public class OAuthProviderController {

    @Autowired
    private OAuthProviderService oauthProviderService;

    /**
     * Link OAuth provider to user
     * POST /api/auth/oauth/providers/link
     *
     * @param userId the user ID
     * @param provider the OAuth provider type (GOOGLE, GITHUB)
     * @param providerUserId the provider's user ID
     * @param providerEmail the provider's email
     * @param providerName the provider's name
     * @return ResponseEntity with OAuthProviderResponse
     */
    @PostMapping("/link")
    public ResponseEntity<OAuthProviderResponse> linkOAuthProvider(
            @RequestParam Integer userId,
            @RequestParam String provider,
            @RequestParam String providerUserId,
            @RequestParam String providerEmail,
            @RequestParam(required = false) String providerName) {
        
        OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        OAuthProviderResponse response = oauthProviderService.linkOAuthProvider(
                userId, oauthProvider, providerUserId, providerEmail, providerName);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unlink OAuth provider from user
     * DELETE /api/auth/oauth/providers/{provider}
     *
     * @param userId the user ID
     * @param provider the OAuth provider type (GOOGLE, GITHUB)
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{provider}")
    public ResponseEntity<Map<String, String>> unlinkOAuthProvider(
            @RequestParam Integer userId,
            @PathVariable String provider) {
        
        OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        oauthProviderService.unlinkOAuthProvider(userId, oauthProvider);
        
        return ResponseEntity.ok(Map.of("message", "OAuth provider unlinked successfully"));
    }

    /**
     * Get all OAuth providers linked to user
     * GET /api/auth/oauth/providers
     *
     * @param userId the user ID
     * @return ResponseEntity with list of OAuthProviderResponse
     */
    @GetMapping
    public ResponseEntity<List<OAuthProviderResponse>> getUserOAuthProviders(
            @RequestParam Integer userId) {
        
        List<OAuthProviderResponse> providers = oauthProviderService.getUserOAuthProviders(userId);
        return ResponseEntity.ok(providers);
    }

    /**
     * Check if OAuth provider is linked to user
     * GET /api/auth/oauth/providers/check
     *
     * @param userId the user ID
     * @param provider the OAuth provider type (GOOGLE, GITHUB)
     * @return ResponseEntity with check result
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkOAuthProviderLink(
            @RequestParam Integer userId,
            @RequestParam String provider) {
        
        OAuthProvider oauthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        boolean isLinked = oauthProviderService.isOAuthProviderLinked(userId, oauthProvider);
        
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "provider", provider,
                "isLinked", isLinked
        ));
    }
}
