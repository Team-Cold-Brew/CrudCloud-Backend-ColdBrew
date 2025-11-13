package com.riwi.CrudCloud.auth.service;

import com.riwi.CrudCloud.auth.dto.response.OAuthProviderResponse;
import com.riwi.CrudCloud.auth.exception.AuthException;
import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.models.UserOAuthProvider;
import com.riwi.CrudCloud.auth.repository.UserOAuthProviderRepository;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for OAuth provider management
 */
@Service
public class OAuthProviderService {

    @Autowired
    private UserOAuthProviderRepository userOAuthProviderRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Link OAuth provider to user
     *
     * @param userId the user ID
     * @param provider the OAuth provider type
     * @param providerUserId the provider's user ID
     * @param providerEmail the provider's email
     * @param providerName the provider's name
     * @return OAuthProviderResponse
     * @throws AuthException if provider user ID is already linked to another user
     */
    @Transactional
    public OAuthProviderResponse linkOAuthProvider(Integer userId, OAuthProvider provider,
                                                   String providerUserId, String providerEmail,
                                                   String providerName) {
        // Check if provider user ID is already linked
        if (userOAuthProviderRepository.existsByProviderUserId(providerUserId)) {
            throw new AuthException("This " + provider.getDisplayName() + " account is already linked to another user",
                    "OAUTH_PROVIDER_ALREADY_LINKED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));

        // Check if this provider is already linked to this user
        if (userOAuthProviderRepository.isOAuthProviderLinked(userId, provider)) {
            throw new AuthException("This user already has a " + provider.getDisplayName() + " account linked",
                    "OAUTH_PROVIDER_ALREADY_LINKED_TO_USER");
        }

        UserOAuthProvider oauthProvider = UserOAuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .providerName(providerName)
                .build();

        UserOAuthProvider savedProvider = userOAuthProviderRepository.save(oauthProvider);

        // Update user's primary OAuth provider if not set
        if (user.getOauthProvider() == null) {
            user.setOauthProvider(provider);
            userRepository.save(user);
        }

        return mapToOAuthProviderResponse(savedProvider);
    }

    /**
     * Unlink OAuth provider from user
     *
     * @param userId the user ID
     * @param provider the OAuth provider type
     * @throws AuthException if provider not found or user has no password
     */
    @Transactional
    public void unlinkOAuthProvider(Integer userId, OAuthProvider provider) {
        UserOAuthProvider oauthProvider = userOAuthProviderRepository
                .findByUserUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new AuthException("OAuth provider not linked to this user", "OAUTH_PROVIDER_NOT_LINKED"));

        User user = oauthProvider.getUser();

        // Prevent unlinking if user has no password (would lock them out)
        if (!user.isPasswordUser()) {
            throw new AuthException("Cannot unlink the only authentication method. Please set a password first.",
                    "CANNOT_UNLINK_ONLY_AUTH_METHOD");
        }

        userOAuthProviderRepository.delete(oauthProvider);

        // If this was the primary OAuth provider, reset it
        if (provider.equals(user.getOauthProvider())) {
            List<UserOAuthProvider> remainingProviders = userOAuthProviderRepository.findByUserUserId(userId);
            if (remainingProviders.isEmpty()) {
                user.setOauthProvider(null);
            } else {
                user.setOauthProvider(remainingProviders.get(0).getProvider());
            }
            userRepository.save(user);
        }
    }

    /**
     * Get all OAuth providers linked to a user
     *
     * @param userId the user ID
     * @return List of OAuthProviderResponse
     */
    @Transactional(readOnly = true)
    public List<OAuthProviderResponse> getUserOAuthProviders(Integer userId) {
        return userOAuthProviderRepository.findByUserUserId(userId)
                .stream()
                .map(this::mapToOAuthProviderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Find user by OAuth provider
     *
     * @param provider the OAuth provider type
     * @param providerUserId the provider's user ID
     * @return User if found, null otherwise
     */
    @Transactional(readOnly = true)
    public User findUserByOAuthProvider(OAuthProvider provider, String providerUserId) {
        return userOAuthProviderRepository.findByProviderUserId(providerUserId)
                .map(UserOAuthProvider::getUser)
                .orElse(null);
    }

    /**
     * Check if OAuth provider is linked to user
     *
     * @param userId the user ID
     * @param provider the OAuth provider type
     * @return true if provider is linked
     */
    @Transactional(readOnly = true)
    public boolean isOAuthProviderLinked(Integer userId, OAuthProvider provider) {
        return userOAuthProviderRepository.isOAuthProviderLinked(userId, provider);
    }

    /**
     * Map UserOAuthProvider entity to OAuthProviderResponse DTO
     *
     * @param provider the UserOAuthProvider entity
     * @return OAuthProviderResponse
     */
    private OAuthProviderResponse mapToOAuthProviderResponse(UserOAuthProvider provider) {
        return new OAuthProviderResponse(
                provider.getProviderId(),
                provider.getProvider().toString(),
                provider.getProviderEmail(),
                provider.getProviderName(),
                provider.getLinkedAt()
        );
    }
}
