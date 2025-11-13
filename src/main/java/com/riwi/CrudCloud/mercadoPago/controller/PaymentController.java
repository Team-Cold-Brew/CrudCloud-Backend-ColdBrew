package com.riwi.CrudCloud.mercadoPago.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.mercadoPago.dto.request.CreatePreferenceRequest;
import com.riwi.CrudCloud.mercadoPago.dto.response.CheckoutProResponse;
import com.riwi.CrudCloud.mercadoPago.dto.response.TransactionResponse;
import com.riwi.CrudCloud.mercadoPago.dto.response.WebhookResponse;
import com.riwi.CrudCloud.mercadoPago.service.PaymentService;

import jakarta.validation.Valid;

/**
 * Controller for payment processing endpoints
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Create payment preference for Checkout Pro
     * POST /api/payments/create-preference
     *
     * @param request the preference creation request
     * @return ResponseEntity with CheckoutProResponse
     */
    @PostMapping("/create-preference")
    public ResponseEntity<CheckoutProResponse> createPreference(@Valid @RequestBody CreatePreferenceRequest request) {
        CheckoutProResponse response = paymentService.createPreference(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Handle MercadoPago webhook notifications
     * POST /api/payments/webhook
     *
     * @param webhookData the webhook payload from MercadoPago
     * @return ResponseEntity with WebhookResponse
     */
    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        WebhookResponse response = paymentService.processWebhook(webhookData);
        
        if ("error".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for a user
     * GET /api/payments/history
     *
     * @param userId the user ID (should come from JWT token in a real scenario)
     * @return ResponseEntity with list of TransactionResponse
     */
    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@RequestParam Integer userId) {
        List<TransactionResponse> transactions = paymentService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get specific transaction details
     * GET /api/payments/transaction/{id}
     *
     * @param transactionId the transaction ID
     * @return ResponseEntity with TransactionResponse
     */
    @GetMapping("/transaction/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Integer id) {
        TransactionResponse transaction = paymentService.getTransaction(id);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get payment status by transaction ID
     * GET /api/payments/status/{id}
     *
     * @param transactionId the transaction ID
     * @return ResponseEntity with payment status
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable Integer id) {
        TransactionResponse transaction = paymentService.getTransaction(id);
        
        Map<String, Object> status = Map.of(
            "transactionId", transaction.getTransactionId(),
            "status", transaction.getStatus(),
            "amount", transaction.getAmount(),
            "currency", transaction.getCurrency(),
            "createdAt", transaction.getCreatedAt(),
            "approvalDate", transaction.getApprovalDate()
        );
        
        return ResponseEntity.ok(status);
    }

    /**
     * Health check endpoint
     * GET /api/payments/health
     *
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = Map.of(
            "status", "UP",
            "module", "mercadoPago",
            "message", "MercadoPago integration is running",
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify configuration
     * GET /api/payments/config-test
     *
     * @return configuration status
     */
    @GetMapping("/config-test")
    public ResponseEntity<Map<String, Object>> configTest() {
        Map<String, Object> response = Map.of(
            "status", "OK",
            "mercadoPagoConfigured", true,
            "endpoints", List.of(
                "POST /api/payments/create-preference",
                "POST /api/payments/webhook", 
                "GET /api/payments/history?userId={id}",
                "GET /api/payments/transaction/{id}",
                "GET /api/payments/status/{id}"
            ),
            "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}
