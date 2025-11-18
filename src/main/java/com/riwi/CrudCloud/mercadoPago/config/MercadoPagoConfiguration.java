package com.riwi.CrudCloud.mercadoPago.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for MercadoPago SDK
 */
@Configuration
@Slf4j
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    /**
     * Initialize MercadoPago SDK with access token
     */
    @PostConstruct
    public void init() {
        log.info("Initializing MercadoPago SDK...");
        
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("MercadoPago access token is not configured!");
            throw new IllegalStateException("MercadoPago access token must be configured in application.properties");
        }
        
        // Set the access token for MercadoPago SDK
        MercadoPagoConfig.setAccessToken(accessToken);
        
        log.info("MercadoPago SDK initialized successfully");
    }
}
