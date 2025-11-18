package com.riwi.CrudCloud.mercadoPago.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.riwi.CrudCloud.auth.repository.PlanRepository;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.common.models.Plan;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.BadRequestException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;
import com.riwi.CrudCloud.common.util.exception.classes.payment.MercadoPagoException;
import com.riwi.CrudCloud.mercadoPago.dto.request.CheckoutRequest;
import com.riwi.CrudCloud.mercadoPago.dto.response.CheckoutResponse;
import com.riwi.CrudCloud.mercadoPago.model.PaymentPreference;
import com.riwi.CrudCloud.mercadoPago.repository.PaymentPreferenceRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for MercadoPago Checkout Pro integration
 */
@Service
@Slf4j
public class MercadoPagoService {

    @Autowired
    private PaymentPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Value("${mercadopago.notification.url}")
    private String notificationUrl;

    @Value("${mercadopago.success.url}")
    private String successUrl;

    @Value("${mercadopago.failure.url}")
    private String failureUrl;

    @Value("${mercadopago.pending.url}")
    private String pendingUrl;

    /**
     * Create a Checkout Pro preference
     *
     * @param request the checkout request
     * @return CheckoutResponse with preference details
     * @throws ResourceNotFoundException if user or plan not found
     * @throws MercadoPagoException if MercadoPago API call fails
     */
    @Transactional
    public CheckoutResponse createCheckoutPreference(CheckoutRequest request) {
        log.info("Creating checkout preference for user {} and plan {}", request.getUserId(), request.getPlanId());

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));

        // Validate plan exists
        Plan plan = planRepository.findById(request.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + request.getPlanId()));

        // Validate quantity
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        try {
            // Generate external reference if not provided
            String externalReference = request.getExternalReference() != null 
                ? request.getExternalReference() 
                : "PLAN-" + plan.getPlanId() + "-USER-" + user.getUserId() + "-" + UUID.randomUUID().toString().substring(0, 8);

            // Create preference item
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(plan.getPlanId().toString())
                .title(plan.getName())
                .description(plan.getDescription())
                .categoryId("services")
                .quantity(quantity)
                .currencyId("ARS")
                .unitPrice(plan.getPrice())
                .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(item);

            // Create back URLs
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

            // Create preference request
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(externalReference)
                .notificationUrl(notificationUrl)
                .statementDescriptor("CrudCloud - " + plan.getName())
                .expirationDateTo(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                .build();

            // Create preference using MercadoPago SDK
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("Preference created successfully: {}", preference.getId());

            // Save preference to database
            PaymentPreference paymentPreference = PaymentPreference.builder()
                .mercadopagoPreferenceId(preference.getId())
                .userId(user.getUserId())
                .planId(plan.getPlanId())
                .title(plan.getName())
                .description(plan.getDescription())
                .quantity(quantity)
                .unitPrice(plan.getPrice())
                .currencyId("ARS")
                .externalReference(externalReference)
                .initPoint(preference.getInitPoint())
                .sandboxInitPoint(preference.getSandboxInitPoint())
                .isExpired(false)
                .expiresAt(preference.getExpirationDateTo() != null 
                    ? LocalDateTime.ofInstant(preference.getExpirationDateTo().toInstant(), ZoneOffset.UTC)
                    : LocalDateTime.now().plusDays(1))
                .build();

            preferenceRepository.save(paymentPreference);

            // Return response
            return CheckoutResponse.builder()
                .preferenceId(preference.getId())
                .initPoint(preference.getInitPoint())
                .sandboxInitPoint(preference.getSandboxInitPoint())
                .externalReference(externalReference)
                .message("Checkout preference created successfully")
                .build();

        } catch (MPApiException e) {
            log.error("MercadoPago API error: {} - {}", e.getStatusCode(), e.getApiResponse().getContent(), e);
            throw new MercadoPagoException("MercadoPago API error: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("MercadoPago SDK error: {}", e.getMessage(), e);
            throw new MercadoPagoException("MercadoPago SDK error: " + e.getMessage());
        }
    }

    /**
     * Get preference by ID
     *
     * @param preferenceId the MercadoPago preference ID
     * @return PaymentPreference
     * @throws ResourceNotFoundException if preference not found
     */
    @Transactional(readOnly = true)
    public PaymentPreference getPreferenceById(String preferenceId) {
        return preferenceRepository.findByMercadopagoPreferenceId(preferenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Preference not found with ID: " + preferenceId));
    }

    /**
     * Get all preferences by user ID
     *
     * @param userId the user ID
     * @return List of PaymentPreference
     */
    @Transactional(readOnly = true)
    public List<PaymentPreference> getPreferencesByUserId(Integer userId) {
        return preferenceRepository.findByUserId(userId);
    }
}
