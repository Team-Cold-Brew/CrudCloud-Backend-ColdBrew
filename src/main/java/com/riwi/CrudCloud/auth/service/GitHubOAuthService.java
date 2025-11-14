package com.riwi.CrudCloud.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riwi.CrudCloud.auth.config.OAuthProviderConfig;
import com.riwi.CrudCloud.auth.dto.response.OAuthUserResponse;
import com.riwi.CrudCloud.auth.dto.response.OAuth2TokenResponse;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.OAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub OAuth Service Implementation
 * Handles token exchange and user profile fetching from GitHub
 */
@Service("githubOAuthService")
@Slf4j
public class GitHubOAuthService implements OAuthService {

    @Autowired
    private OAuthProviderConfig oauthConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public OAuth2TokenResponse exchangeCodeForToken(String code) {
        log.debug("Exchanging authorization code for access token with GitHub");

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("code", code);
            requestBody.put("client_id", oauthConfig.getGithubClientId());
            requestBody.put("client_secret", oauthConfig.getGithubClientSecret());
            requestBody.put("redirect_uri", oauthConfig.getRedirectUri());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            headers.set("Accept", "application/json");

            ResponseEntity<String> response = restTemplate.exchange(
                oauthConfig.getGithubTokenUri(),
                HttpMethod.POST,
                new HttpEntity<>(convertMapToFormData(requestBody), headers),
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new OAuthException("INVALID_CODE", "Failed to exchange authorization code with GitHub");
            }

            JsonNode responseBody = objectMapper.readTree(response.getBody());

            if (responseBody.has("error")) {
                throw new OAuthException("INVALID_CODE",
                    "GitHub OAuth error: " + responseBody.get("error_description").asText());
            }

            OAuth2TokenResponse tokenResponse = OAuth2TokenResponse.builder()
                .accessToken(responseBody.get("access_token").asText())
                .tokenType(responseBody.get("token_type").asText())
                .scope(responseBody.has("scope") ? responseBody.get("scope").asText() : "")
                .build();

            log.debug("Successfully exchanged authorization code for GitHub access token");
            return tokenResponse;

        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error exchanging authorization code with GitHub", e);
            throw new OAuthException("TOKEN_EXCHANGE_FAILED",
                "Failed to exchange authorization code with GitHub", e);
        }
    }

    @Override
    public OAuthUserResponse getUserProfile(String accessToken) {
        log.debug("Fetching user profile from GitHub");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");

            // Fetch user profile
            ResponseEntity<String> response = restTemplate.exchange(
                oauthConfig.getGithubUserInfoUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new OAuthException("PROFILE_FETCH_FAILED", "Failed to fetch user profile from GitHub");
            }

            JsonNode profileNode = objectMapper.readTree(response.getBody());
            
            String email = profileNode.has("email") && !profileNode.get("email").isNull() 
                ? profileNode.get("email").asText()
                : fetchPrimaryEmail(headers);

            // Parse name into firstName and lastName
            String fullName = profileNode.has("name") ? profileNode.get("name").asText() : "";
            String[] nameParts = fullName.split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            OAuthUserResponse userResponse = OAuthUserResponse.builder()
                .providerId(String.valueOf(profileNode.get("id").asInt()))
                .email(email)
                .name(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .profilePictureUrl(profileNode.has("avatar_url") ? profileNode.get("avatar_url").asText() : "")
                .build();

            log.debug("Successfully fetched user profile from GitHub: {}", userResponse.getEmail());
            return userResponse;

        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user profile from GitHub", e);
            throw new OAuthException("PROFILE_FETCH_FAILED", "Failed to fetch user profile from GitHub", e);
        }
    }

    /**
     * Fetch primary email from GitHub if not available in profile
     */
    private String fetchPrimaryEmail(HttpHeaders headers) throws Exception {
        log.debug("Fetching primary email from GitHub");

        ResponseEntity<String> response = restTemplate.exchange(
            oauthConfig.getGithubUserEmailUri(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode emailsNode = objectMapper.readTree(response.getBody());
            if (emailsNode.isArray() && emailsNode.size() > 0) {
                for (JsonNode emailNode : emailsNode) {
                    if (emailNode.get("primary").asBoolean()) {
                        return emailNode.get("email").asText();
                    }
                }
                // If no primary email found, return the first one
                return emailsNode.get(0).get("email").asText();
            }
        }

        throw new OAuthException("EMAIL_FETCH_FAILED", "Could not fetch email from GitHub");
    }

    /**
     * Convert map to URL-encoded form data
     */
    private String convertMapToFormData(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
