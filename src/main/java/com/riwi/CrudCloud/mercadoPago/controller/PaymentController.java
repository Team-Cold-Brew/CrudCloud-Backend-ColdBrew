package com.riwi.CrudCloud.mercadoPago.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.mercadoPago.dto.request.CheckoutRequest;
import com.riwi.CrudCloud.mercadoPago.dto.response.CheckoutResponse;
import com.riwi.CrudCloud.mercadoPago.dto.response.PaymentResponse;
import com.riwi.CrudCloud.mercadoPago.service.MercadoPagoService;
import com.riwi.CrudCloud.mercadoPago.service.PaymentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for payment operations
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Autowired
    private PaymentService paymentService;

    /**
     * Create checkout preference
     * POST /api/payments/checkout
     *
     * @param request the checkout request
     * @return ResponseEntity with CheckoutResponse
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(@Valid @RequestBody CheckoutRequest request) {
        log.info("Creating checkout for user {} and plan {}", request.getUserId(), request.getPlanId());
        CheckoutResponse response = mercadoPagoService.createCheckoutPreference(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get payment by ID
     * GET /api/payments/{paymentId}
     *
     * @param paymentId the payment ID
     * @return ResponseEntity with PaymentResponse
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        log.info("Getting payment by ID: {}", paymentId);
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by MercadoPago payment ID
     * GET /api/payments/mercadopago/{mercadopagoPaymentId}
     *
     * @param mercadopagoPaymentId the MercadoPago payment ID
     * @return ResponseEntity with PaymentResponse
     */
    @GetMapping("/mercadopago/{mercadopagoPaymentId}")
    public ResponseEntity<PaymentResponse> getPaymentByMercadoPagoId(@PathVariable String mercadopagoPaymentId) {
        log.info("Getting payment by MercadoPago ID: {}", mercadopagoPaymentId);
        PaymentResponse response = paymentService.getPaymentByMercadoPagoId(mercadopagoPaymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments by user ID
     * GET /api/payments/user/{userId}
     *
     * @param userId the user ID
     * @return ResponseEntity with List of PaymentResponse
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUserId(@PathVariable Integer userId) {
        log.info("Getting payments for user: {}", userId);
        List<PaymentResponse> responses = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all approved payments by user ID
     * GET /api/payments/user/{userId}/approved
     *
     * @param userId the user ID
     * @return ResponseEntity with List of PaymentResponse
     */
    @GetMapping("/user/{userId}/approved")
    public ResponseEntity<List<PaymentResponse>> getApprovedPaymentsByUserId(@PathVariable Integer userId) {
        log.info("Getting approved payments for user: {}", userId);
        List<PaymentResponse> responses = paymentService.getApprovedPaymentsByUserId(userId);
        return ResponseEntity.ok(responses);
    }
}
