package com.riwi.CrudCloud.mercadoPago.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.common.models.Payment;
import com.riwi.CrudCloud.common.models.Plan;
import com.riwi.CrudCloud.common.models.Subscription;
import com.riwi.CrudCloud.common.models.SubscriptionStatus;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;
import com.riwi.CrudCloud.mercadoPago.dto.response.SubscriptionResponse;
import com.riwi.CrudCloud.mercadoPago.repository.SubscriptionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for subscription management
 */
@Service
@Slf4j
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Create or update subscription after successful payment
     *
     * @param user the user
     * @param plan the plan
     * @param payment the payment
     * @return Subscription
     */
    @Transactional
    public Subscription createOrUpdateSubscription(User user, Plan plan, Payment payment) {
        log.info("Creating/updating subscription for user {} and plan {}", user.getUserId(), plan.getPlanId());

        // Check if user already has an active subscription
        Subscription existingSubscription = subscriptionRepository.findActiveSubscriptionByUserId(user.getUserId())
            .orElse(null);

        if (existingSubscription != null) {
            // Update existing subscription
            log.info("Updating existing subscription {}", existingSubscription.getSubscriptionId());
            
            // Extend subscription based on plan billing cycle
            if ("monthly".equalsIgnoreCase(plan.getBillingCycle())) {
                existingSubscription.setEndDate(existingSubscription.getEndDate().plusMonths(1));
                existingSubscription.setNextBillingDate(existingSubscription.getEndDate());
            } else if ("yearly".equalsIgnoreCase(plan.getBillingCycle())) {
                existingSubscription.setEndDate(existingSubscription.getEndDate().plusYears(1));
                existingSubscription.setNextBillingDate(existingSubscription.getEndDate());
            }

            existingSubscription.setStatus(SubscriptionStatus.ACTIVE);
            return subscriptionRepository.save(existingSubscription);
        } else {
            // Create new subscription
            log.info("Creating new subscription for user {} and plan {}", user.getUserId(), plan.getPlanId());

            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate;
            LocalDateTime nextBillingDate;

            // Calculate end date and next billing date based on plan billing cycle
            if ("monthly".equalsIgnoreCase(plan.getBillingCycle())) {
                endDate = startDate.plusMonths(1);
                nextBillingDate = endDate;
            } else if ("yearly".equalsIgnoreCase(plan.getBillingCycle())) {
                endDate = startDate.plusYears(1);
                nextBillingDate = endDate;
            } else {
                // Default to monthly
                endDate = startDate.plusMonths(1);
                nextBillingDate = endDate;
            }

            Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .nextBillingDate(nextBillingDate)
                .autoRenewal(true)
                .build();

            return subscriptionRepository.save(subscription);
        }
    }

    /**
     * Get subscription by ID
     *
     * @param subscriptionId the subscription ID
     * @return SubscriptionResponse
     * @throws ResourceNotFoundException if subscription not found
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Get all subscriptions by user ID
     *
     * @param userId the user ID
     * @return List of SubscriptionResponse
     */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByUserId(Integer userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
            .map(this::mapToSubscriptionResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get active subscription by user ID
     *
     * @param userId the user ID
     * @return SubscriptionResponse
     * @throws ResourceNotFoundException if no active subscription found
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscriptionByUserId(Integer userId) {
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for user ID: " + userId));

        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Cancel subscription
     *
     * @param subscriptionId the subscription ID
     * @return SubscriptionResponse
     * @throws ResourceNotFoundException if subscription not found
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long subscriptionId) {
        log.info("Cancelling subscription {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + subscriptionId));

        subscription.cancel();
        subscriptionRepository.save(subscription);

        log.info("Subscription {} cancelled successfully", subscriptionId);
        return mapToSubscriptionResponse(subscription);
    }

    /**
     * Check if user has active subscription
     *
     * @param userId the user ID
     * @return true if user has active subscription
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Integer userId) {
        return subscriptionRepository.hasActiveSubscription(userId);
    }

    /**
     * Get all subscriptions that need renewal
     *
     * @return List of Subscription
     */
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionsNeedingRenewal() {
        return subscriptionRepository.findSubscriptionsNeedingRenewal();
    }

    /**
     * Map Subscription entity to SubscriptionResponse DTO
     */
    private SubscriptionResponse mapToSubscriptionResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
            .subscriptionId(subscription.getSubscriptionId())
            .userId(subscription.getUser().getUserId())
            .planId(subscription.getPlan().getPlanId())
            .planName(subscription.getPlan().getName())
            .status(subscription.getStatus())
            .startDate(subscription.getStartDate())
            .endDate(subscription.getEndDate())
            .nextBillingDate(subscription.getNextBillingDate())
            .autoRenewal(subscription.getAutoRenewal())
            .createdAt(subscription.getCreatedAt())
            .build();
    }
}
