package com.riwi.CrudCloud.auth.repository;

import com.riwi.CrudCloud.common.models.OAuthProvider;
import com.riwi.CrudCloud.common.models.UserOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProvider, Integer> {

    /**
     * Find OAuth provider by provider user ID
     *
     * @param providerUserId the provider's user ID
     * @return Optional containing the OAuth provider if found
     */
    Optional<UserOAuthProvider> findByProviderUserId(String providerUserId);

    /**
     * Find OAuth provider by user ID and provider type
     *
     * @param userId the user ID
     * @param provider the OAuth provider type
     * @return Optional containing the OAuth provider if found
     */
    Optional<UserOAuthProvider> findByUserUserIdAndProvider(Integer userId, OAuthProvider provider);

    /**
     * Find all OAuth providers linked to a user
     *
     * @param userId the user ID
     * @return List of OAuth providers linked to the user
     */
    List<UserOAuthProvider> findByUserUserId(Integer userId);

    /**
     * Check if OAuth provider is linked to user
     *
     * @param userId the user ID
     * @param provider the OAuth provider type
     * @return true if provider is linked to user
     */
    @Query("SELECT CASE WHEN COUNT(uop) > 0 THEN true ELSE false END FROM UserOAuthProvider uop WHERE uop.user.userId = :userId AND uop.provider = :provider")
    boolean isOAuthProviderLinked(@Param("userId") Integer userId, @Param("provider") OAuthProvider provider);

    /**
     * Check if OAuth provider user ID is already registered
     *
     * @param providerUserId the provider user ID
     * @return true if provider user ID is already registered
     */
    @Query("SELECT CASE WHEN COUNT(uop) > 0 THEN true ELSE false END FROM UserOAuthProvider uop WHERE uop.providerUserId = :providerUserId")
    boolean existsByProviderUserId(@Param("providerUserId") String providerUserId);
}
