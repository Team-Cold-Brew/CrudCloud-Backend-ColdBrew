package com.riwi.CrudCloud.auth.service;

import com.riwi.CrudCloud.auth.dto.response.OAuthUserResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuth2TokenResponse;

/**
 * Interface for OAuth Service implementations
 * Defines contract for token exchange and user profile fetching
 */
public interface OAuthService {
    
    /**
     * Exchange authorization code for access token
     * 
     * @param code the authorization code from OAuth provider
     * @return OAuth2TokenResponse containing access token
     */
    OAuth2TokenResponse exchangeCodeForToken(String code);
    
    /**
     * Fetch user profile from OAuth provider using access token
     * 
     * @param accessToken the access token from OAuth provider
     * @return OAuthUserResponse containing user profile information
     */
    OAuthUserResponse getUserProfile(String accessToken);
}
