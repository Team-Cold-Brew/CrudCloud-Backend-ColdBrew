package com.riwi.CrudCloud.mercadoPago.config;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.payment.PaymentClient;

/**
 * Configuration class for MercadoPago integration
 */
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.public-key:}")
    private String publicKey;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    /**
     * Initialize MercadoPago SDK configuration
     */
    @PostConstruct
    public void initializeMercadoPago() {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("MercadoPago access token is not configured. Please set mercadopago.access-token property.");
        }
        
        MercadoPagoConfig.setAccessToken(accessToken);
        
        // Log configuration (without exposing sensitive data)
        System.out.println("MercadoPago configured - Sandbox mode: " + sandbox);
        System.out.println("Access token configured: " + (accessToken.length() > 10 ? "Yes" : "No"));
    }

    /**
     * Bean for creating payment preferences
     */
    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    /**
     * Bean for payment operations
     */
    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }

    // Getters for configuration values
    public String getAccessToken() {
        return accessToken;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public boolean isSandbox() {
        return sandbox;
    }
}
