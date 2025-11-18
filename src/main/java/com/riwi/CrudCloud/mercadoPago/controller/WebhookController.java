package com.riwi.CrudCloud.mercadoPago.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.mercadoPago.dto.request.WebhookRequest;
import com.riwi.CrudCloud.mercadoPago.service.WebhookService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for MercadoPago webhook notifications
 */
@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    /**
     * Handle MercadoPago webhook notifications
     * POST /api/webhooks/mercadopago
     *
     * This endpoint receives notifications from MercadoPago about payment events
     *
     * @param webhookRequest the webhook request from MercadoPago
     * @return ResponseEntity with status
     */
    @PostMapping("/mercadopago")
    public ResponseEntity<String> handleMercadoPagoWebhook(@RequestBody WebhookRequest webhookRequest) {
        log.info("Received MercadoPago webhook: type={}, action={}, id={}", 
            webhookRequest.getType(), webhookRequest.getAction(), webhookRequest.getId());

        try {
            webhookService.processWebhook(webhookRequest);
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Test endpoint for webhook notifications (can be removed in production)
     * POST /api/webhooks/mercadopago/test
     *
     * @param topic the notification topic
     * @param id the resource ID
     * @return ResponseEntity with status
     */
    @PostMapping("/mercadopago/test")
    public ResponseEntity<String> handleTestWebhook(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id) {
        
        log.info("Received test webhook: topic={}, id={}", topic, id);
        
        if ("payment".equals(topic) && id != null) {
            WebhookRequest webhookRequest = WebhookRequest.builder()
                .type("payment")
                .action("payment.updated")
                .data(WebhookRequest.WebhookData.builder().id(id).build())
                .build();
            
            webhookService.processWebhook(webhookRequest);
            return ResponseEntity.ok("Test webhook processed successfully");
        }
        
        return ResponseEntity.ok("Test webhook received");
    }
}
