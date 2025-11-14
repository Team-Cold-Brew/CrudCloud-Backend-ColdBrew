package com.riwi.CrudCloud.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth Provider Configuration
 * Loads OAuth credentials and endpoints from application properties
 */
@Configuration
@Getter
@SuppressWarnings("unused")
public class OAuthProviderConfig {

    // Google OAuth Configuration
    @Value("${oauth.google.client-id:}")
    private String googleClientId;

    @Value("${oauth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth.google.authorization-uri:https://accounts.google.com/o/oauth2/v2/auth}")
    private String googleAuthorizationUri;

    @Value("${oauth.google.token-uri:https://oauth2.googleapis.com/token}")
    private String googleTokenUri;

    @Value("${oauth.google.user-info-uri:https://www.googleapis.com/oauth2/v2/userinfo}")
    private String googleUserInfoUri;

    @Value("${oauth.google.scopes:openid,profile,email}")
    private String googleScopes;

    // GitHub OAuth Configuration
    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    @Value("${oauth.github.authorization-uri:https://github.com/login/oauth/authorize}")
    private String githubAuthorizationUri;

    @Value("${oauth.github.token-uri:https://github.com/login/oauth/access_token}")
    private String githubTokenUri;

    @Value("${oauth.github.user-info-uri:https://api.github.com/user}")
    private String githubUserInfoUri;

    @Value("${oauth.github.user-email-uri:https://api.github.com/user/emails}")
    private String githubUserEmailUri;

    @Value("${oauth.github.scopes:user:email,read:user}")
    private String githubScopes;

    // Common Configuration
    @Value("${oauth.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;
}
