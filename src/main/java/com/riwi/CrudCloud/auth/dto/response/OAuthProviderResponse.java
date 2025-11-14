package com.riwi.CrudCloud.auth.dto.response;

import java.time.LocalDateTime;

public class OAuthProviderResponse {

    private Integer providerId;
    private String provider;
    private String providerEmail;
    private String providerName;
    private LocalDateTime linkedAt;

    // Constructor
    public OAuthProviderResponse(Integer providerId, String provider, String providerEmail, 
                                String providerName, LocalDateTime linkedAt) {
        this.providerId = providerId;
        this.provider = provider;
        this.providerEmail = providerEmail;
        this.providerName = providerName;
        this.linkedAt = linkedAt;
    }

    // Getters and Setters
    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }
}
