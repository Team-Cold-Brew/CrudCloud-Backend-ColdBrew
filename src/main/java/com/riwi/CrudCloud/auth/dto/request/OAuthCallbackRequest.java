package com.riwi.CrudCloud.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OAuth callback requests
 * Contains the authorization code and CSRF state token from OAuth provider
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthCallbackRequest {
    
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    @NotBlank(message = "State token is required for CSRF protection")
    private String state;
}
