package com.riwi.CrudCloud.mercadoPago.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.common.util.exception.classes.payment.WebhookValidationException;
import com.riwi.CrudCloud.mercadoPago.dto.request.WebhookRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling MercadoPago webhooks
 */
@Service
@Slf4j
public class WebhookService {

    @Autowired
    private PaymentService paymentService;

    /**
     * Process webhook notification from MercadoPago
     *
     * @param webhookRequest the webhook request
     */
    @Transactional
    public void processWebhook(WebhookRequest webhookRequest) {
        log.info("Processing webhook: type={}, action={}, id={}", 
            webhookRequest.getType(), webhookRequest.getAction(), webhookRequest.getId());

        try {
            // Handle different webhook types
            if ("payment".equals(webhookRequest.getType())) {
                handlePaymentWebhook(webhookRequest);
            } else {
                log.warn("Unhandled webhook type: {}", webhookRequest.getType());
            }
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handle payment webhook notifications
     */
    private void handlePaymentWebhook(WebhookRequest webhookRequest) {
        String action = webhookRequest.getAction();
        String paymentId = webhookRequest.getData() != null ? webhookRequest.getData().getId() : null;

        if (paymentId == null) {
            log.error("Payment webhook has no payment ID");
            throw new WebhookValidationException(
                "Payment webhook missing payment ID",
                webhookRequest.getType(),
                String.valueOf(webhookRequest.getData())
            );
        }

        log.info("Processing payment webhook: action={}, paymentId={}", action, paymentId);

        switch (action) {
            case "payment.created":
            case "payment.updated":
                // Process or update payment
                paymentService.processPayment(paymentId);
                break;
            default:
                log.info("Payment webhook action not handled: {}", action);
        }
    }
}
