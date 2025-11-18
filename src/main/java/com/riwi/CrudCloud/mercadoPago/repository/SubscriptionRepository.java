package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.common.models.Subscription;
import com.riwi.CrudCloud.common.models.SubscriptionStatus;

/**
 * Repository for Subscription entity
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find all subscriptions by user ID
     *
     * @param userId the user ID
     * @return List of subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.user.userId = :userId ORDER BY s.createdAt DESC")
    List<Subscription> findByUserId(@Param("userId") Integer userId);

    /**
     * Find active subscription by user ID
     *
     * @param userId the user ID
     * @return Optional containing the active subscription if found
     */
    @Query("SELECT s FROM Subscription s WHERE s.user.userId = :userId AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") Integer userId);

    /**
     * Find all subscriptions by plan ID
     *
     * @param planId the plan ID
     * @return List of subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.plan.planId = :planId ORDER BY s.createdAt DESC")
    List<Subscription> findByPlanId(@Param("planId") Integer planId);

    /**
     * Find all subscriptions by status
     *
     * @param status the subscription status
     * @return List of subscriptions
     */
    List<Subscription> findByStatus(SubscriptionStatus status);

    /**
     * Find all active subscriptions
     *
     * @return List of active subscriptions
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    List<Subscription> findAllActiveSubscriptions();

    /**
     * Check if user has active subscription
     *
     * @param userId the user ID
     * @return true if user has active subscription
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s WHERE s.user.userId = :userId AND s.status = 'ACTIVE'")
    boolean hasActiveSubscription(@Param("userId") Integer userId);

    /**
     * Find subscriptions that need renewal (next billing date is today or in the past)
     *
     * @return List of subscriptions that need renewal
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.autoRenewal = true AND s.nextBillingDate <= CURRENT_TIMESTAMP")
    List<Subscription> findSubscriptionsNeedingRenewal();
}
