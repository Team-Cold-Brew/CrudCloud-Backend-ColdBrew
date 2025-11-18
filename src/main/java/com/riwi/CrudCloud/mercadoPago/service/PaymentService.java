package com.riwi.CrudCloud.mercadoPago.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.riwi.CrudCloud.auth.repository.PlanRepository;
import com.riwi.CrudCloud.auth.repository.UserRepository;
import com.riwi.CrudCloud.common.models.Payment;
import com.riwi.CrudCloud.common.models.PaymentStatus;
import com.riwi.CrudCloud.common.models.Plan;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.ResourceNotFoundException;
import com.riwi.CrudCloud.common.util.exception.classes.payment.MercadoPagoException;
import com.riwi.CrudCloud.common.util.exception.classes.payment.PaymentNotFoundException;
import com.riwi.CrudCloud.mercadoPago.dto.response.PaymentResponse;
import com.riwi.CrudCloud.mercadoPago.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for payment operations
 */
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * Process payment notification from MercadoPago
     *
     * @param paymentId the MercadoPago payment ID
     * @throws MercadoPagoException if MercadoPago API call fails
     */
    @Transactional
    public void processPayment(String paymentId) {
        log.info("Processing payment notification for payment ID: {}", paymentId);

        try {
            // Check if payment already exists
            if (paymentRepository.existsByMercadopagoPaymentId(paymentId)) {
                log.info("Payment {} already processed, updating status", paymentId);
                updatePaymentStatus(paymentId);
                return;
            }

            // Get payment details from MercadoPago
            PaymentClient paymentClient = new PaymentClient();
            com.mercadopago.resources.payment.Payment mpPayment = paymentClient.get(Long.parseLong(paymentId));

            log.info("Payment details retrieved: status={}, external_reference={}", 
                mpPayment.getStatus(), mpPayment.getExternalReference());

            // Extract external reference to get user and plan IDs
            String externalReference = mpPayment.getExternalReference();
            if (externalReference == null) {
                log.error("Payment {} has no external reference", paymentId);
                throw new MercadoPagoException("Payment has no external reference");
            }

            // Parse external reference (format: PLAN-{planId}-USER-{userId}-{uuid})
            String[] parts = externalReference.split("-");
            if (parts.length < 4) {
                log.error("Invalid external reference format: {}", externalReference);
                throw new MercadoPagoException("Invalid external reference format");
            }

            Integer planId = Integer.parseInt(parts[1]);
            Integer userId = Integer.parseInt(parts[3]);

            // Validate user and plan
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

            // Map MercadoPago status to our PaymentStatus enum
            PaymentStatus status = mapMercadoPagoStatus(mpPayment.getStatus());

            // Create payment entity
            Payment payment = Payment.builder()
                .mercadopagoPaymentId(paymentId)
                .preferenceId(mpPayment.getOrder() != null ? String.valueOf(mpPayment.getOrder().getId()) : null)
                .user(user)
                .plan(plan)
                .amount(mpPayment.getTransactionAmount())
                .currency(mpPayment.getCurrencyId())
                .status(status)
                .statusDetail(mpPayment.getStatusDetail())
                .paymentMethod(mpPayment.getPaymentMethodId())
                .paymentType(mpPayment.getPaymentTypeId())
                .description(mpPayment.getDescription())
                .externalReference(externalReference)
                .payerEmail(mpPayment.getPayer() != null ? mpPayment.getPayer().getEmail() : null)
                .payerIdentification(mpPayment.getPayer() != null && mpPayment.getPayer().getIdentification() != null 
                    ? mpPayment.getPayer().getIdentification().getNumber() : null)
                .approvedAt(status == PaymentStatus.APPROVED ? LocalDateTime.now() : null)
                .build();

            paymentRepository.save(payment);
            log.info("Payment {} saved successfully", paymentId);

            // If payment is approved, create or update subscription
            if (status == PaymentStatus.APPROVED) {
                subscriptionService.createOrUpdateSubscription(user, plan, payment);
                log.info("Subscription created/updated for user {} and plan {}", userId, planId);
            }

        } catch (MPApiException e) {
            log.error("MercadoPago API error processing payment {}: {} - {}", 
                paymentId, e.getStatusCode(), e.getApiResponse().getContent(), e);
            throw new MercadoPagoException("MercadoPago API error: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("MercadoPago SDK error processing payment {}: {}", paymentId, e.getMessage(), e);
            throw new MercadoPagoException("MercadoPago SDK error: " + e.getMessage());
        } catch (NumberFormatException e) {
            log.error("Invalid payment ID format: {}", paymentId, e);
            throw new MercadoPagoException("Invalid payment ID format");
        }
    }

    /**
     * Update payment status from MercadoPago
     *
     * @param paymentId the MercadoPago payment ID
     */
    @Transactional
    public void updatePaymentStatus(String paymentId) {
        log.info("Updating payment status for payment ID: {}", paymentId);

        try {
            Payment payment = paymentRepository.findByMercadopagoPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

            // Get updated payment details from MercadoPago
            PaymentClient paymentClient = new PaymentClient();
            com.mercadopago.resources.payment.Payment mpPayment = paymentClient.get(Long.parseLong(paymentId));

            // Update payment status
            PaymentStatus newStatus = mapMercadoPagoStatus(mpPayment.getStatus());
            PaymentStatus oldStatus = payment.getStatus();

            payment.setStatus(newStatus);
            payment.setStatusDetail(mpPayment.getStatusDetail());

            if (newStatus == PaymentStatus.APPROVED && payment.getApprovedAt() == null) {
                payment.setApprovedAt(LocalDateTime.now());
            }

            paymentRepository.save(payment);
            log.info("Payment {} status updated from {} to {}", paymentId, oldStatus, newStatus);

            // If payment was just approved, create or update subscription
            if (newStatus == PaymentStatus.APPROVED && oldStatus != PaymentStatus.APPROVED) {
                subscriptionService.createOrUpdateSubscription(payment.getUser(), payment.getPlan(), payment);
                log.info("Subscription created/updated for user {} and plan {}", 
                    payment.getUser().getUserId(), payment.getPlan().getPlanId());
            }

        } catch (MPApiException e) {
            log.error("MercadoPago API error updating payment {}: {} - {}", 
                paymentId, e.getStatusCode(), e.getApiResponse().getContent(), e);
            throw new MercadoPagoException("MercadoPago API error: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("MercadoPago SDK error updating payment {}: {}", paymentId, e.getMessage(), e);
            throw new MercadoPagoException("MercadoPago SDK error: " + e.getMessage());
        }
    }

    /**
     * Get payment by ID
     *
     * @param paymentId the payment ID
     * @return PaymentResponse
     * @throws PaymentNotFoundException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        return mapToPaymentResponse(payment);
    }

    /**
     * Get payment by MercadoPago payment ID
     *
     * @param mercadopagoPaymentId the MercadoPago payment ID
     * @return PaymentResponse
     * @throws PaymentNotFoundException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByMercadoPagoId(String mercadopagoPaymentId) {
        Payment payment = paymentRepository.findByMercadopagoPaymentId(mercadopagoPaymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with MercadoPago ID: " + mercadopagoPaymentId));

        return mapToPaymentResponse(payment);
    }

    /**
     * Get all payments by user ID
     *
     * @param userId the user ID
     * @return List of PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Integer userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
            .map(this::mapToPaymentResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all approved payments by user ID
     *
     * @param userId the user ID
     * @return List of PaymentResponse
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getApprovedPaymentsByUserId(Integer userId) {
        List<Payment> payments = paymentRepository.findApprovedPaymentsByUserId(userId);
        return payments.stream()
            .map(this::mapToPaymentResponse)
            .collect(Collectors.toList());
    }

    /**
     * Map MercadoPago status to our PaymentStatus enum
     */
    private PaymentStatus mapMercadoPagoStatus(String mpStatus) {
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "in_process" -> PaymentStatus.IN_PROCESS;
            case "in_mediation" -> PaymentStatus.IN_MEDIATION;
            case "charged_back" -> PaymentStatus.CHARGED_BACK;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Map Payment entity to PaymentResponse DTO
     */
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .mercadopagoPaymentId(payment.getMercadopagoPaymentId())
            .userId(payment.getUser().getUserId())
            .planId(payment.getPlan().getPlanId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus())
            .statusDetail(payment.getStatusDetail())
            .paymentMethod(payment.getPaymentMethod())
            .paymentType(payment.getPaymentType())
            .description(payment.getDescription())
            .externalReference(payment.getExternalReference())
            .payerEmail(payment.getPayerEmail())
            .approvedAt(payment.getApprovedAt())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
