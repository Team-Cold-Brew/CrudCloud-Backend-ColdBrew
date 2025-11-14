package com.riwi.CrudCloud.common.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_oauth_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_id")
    private Integer providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, unique = true, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    protected void onCreate() {
        this.linkedAt = LocalDateTime.now();
    }

    /**
     * Get display name for the OAuth connection
     */
    public String getDisplayName() {
        return provider.getDisplayName() + " (" + providerEmail + ")";
    }
}
