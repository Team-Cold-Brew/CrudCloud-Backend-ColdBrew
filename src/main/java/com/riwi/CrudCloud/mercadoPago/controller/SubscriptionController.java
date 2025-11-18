package com.riwi.CrudCloud.mercadoPago.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.mercadoPago.dto.response.SubscriptionResponse;
import com.riwi.CrudCloud.mercadoPago.service.SubscriptionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for subscription operations
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
@Slf4j
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Get subscription by ID
     * GET /api/subscriptions/{subscriptionId}
     *
     * @param subscriptionId the subscription ID
     * @return ResponseEntity with SubscriptionResponse
     */
    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable Long subscriptionId) {
        log.info("Getting subscription by ID: {}", subscriptionId);
        SubscriptionResponse response = subscriptionService.getSubscriptionById(subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all subscriptions by user ID
     * GET /api/subscriptions/user/{userId}
     *
     * @param userId the user ID
     * @return ResponseEntity with List of SubscriptionResponse
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByUserId(@PathVariable Integer userId) {
        log.info("Getting subscriptions for user: {}", userId);
        List<SubscriptionResponse> responses = subscriptionService.getSubscriptionsByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get active subscription by user ID
     * GET /api/subscriptions/user/{userId}/active
     *
     * @param userId the user ID
     * @return ResponseEntity with SubscriptionResponse
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<SubscriptionResponse> getActiveSubscriptionByUserId(@PathVariable Integer userId) {
        log.info("Getting active subscription for user: {}", userId);
        SubscriptionResponse response = subscriptionService.getActiveSubscriptionByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel subscription
     * DELETE /api/subscriptions/{subscriptionId}
     *
     * @param subscriptionId the subscription ID
     * @return ResponseEntity with SubscriptionResponse
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable Long subscriptionId) {
        log.info("Cancelling subscription: {}", subscriptionId);
        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if user has active subscription
     * GET /api/subscriptions/user/{userId}/has-active
     *
     * @param userId the user ID
     * @return ResponseEntity with boolean
     */
    @GetMapping("/user/{userId}/has-active")
    public ResponseEntity<Boolean> hasActiveSubscription(@PathVariable Integer userId) {
        log.info("Checking if user {} has active subscription", userId);
        boolean hasActive = subscriptionService.hasActiveSubscription(userId);
        return ResponseEntity.ok(hasActive);
    }
}
