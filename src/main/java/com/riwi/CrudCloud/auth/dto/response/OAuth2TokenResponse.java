package com.riwi.CrudCloud.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OAuth 2.0 token exchange response
 * Represents the response from OAuth provider's token endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2TokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    private String scope;
}
