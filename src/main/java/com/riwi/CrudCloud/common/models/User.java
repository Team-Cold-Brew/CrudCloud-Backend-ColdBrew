package com.riwi.CrudCloud.common.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    @Column(name = "first_name", nullable = true, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = true, length = 100)
    private String lastName;

    @Column(name = "profile_picture_url", nullable = true, length = 500)
    private String profilePictureUrl;

    @Column(name = "google_id", unique = true, nullable = true, length = 255)
    private String googleId;

    @Column(name = "github_id", unique = true, nullable = true, length = 255)
    private String githubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = true, length = 20)
    private OAuthProvider oauthProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_plan_id")
    private Plan personalPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Soft delete method
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.INACTIVE;
    }

    /**
     * Check if user is deleted (soft delete)
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Check if user registered via OAuth
     */
    public boolean isOAuthUser() {
        return this.oauthProvider != null;
    }

    /**
     * Check if user registered with password
     */
    public boolean isPasswordUser() {
        return this.password != null;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return null;
    }
}
