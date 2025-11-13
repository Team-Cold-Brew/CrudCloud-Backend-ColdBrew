package com.riwi.CrudCloud.auth.model;

import lombok.Getter;

/**
 * OAuth Provider enum to identify which OAuth provider is being used
 */
@Getter
public enum OAuthProvider {
    GOOGLE("google"),
    GITHUB("github"),
    MANUAL("manual");

    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

    /**
     * Get OAuthProvider from string value
     * 
     * @param value the provider value
     * @return OAuthProvider enum value
     */
    public static OAuthProvider fromValue(String value) {
        for (OAuthProvider provider : OAuthProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Invalid OAuth provider: " + value);
    }
}
