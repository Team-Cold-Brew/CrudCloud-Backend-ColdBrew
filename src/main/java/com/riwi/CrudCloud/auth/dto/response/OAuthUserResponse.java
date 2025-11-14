package com.riwi.CrudCloud.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a user's OAuth profile information
 * Extracted from OAuth provider's user info endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthUserResponse {
    
    private String providerId;          // Unique ID from OAuth provider
    private String email;
    private String name;                // Full name
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
}
