package com.riwi.CrudCloud.common.util.exception.classes.payment;

import org.springframework.http.HttpStatus;

import com.riwi.CrudCloud.common.util.exception.classes.client_errors.BadRequestException;

import lombok.Getter;

/**
 * Exception thrown when webhook validation fails.
 * HTTP Status: 400 Bad Request
 * Category: ClientErrorException (4xx client error)
 * 
 * Used for:
 * - Invalid webhook signature
 * - Missing webhook parameters
 * - Malformed webhook data
 * - Unknown webhook type
 * 
 * Can be caught as: catch (BadRequestException e) { ... } or catch (ClientErrorException e) { ... }
 */
@Getter
public class WebhookValidationException extends BadRequestException {

    private String webhookType;
    private String webhookData;

    public WebhookValidationException(String message) {
        super(message);
    }

    public WebhookValidationException(String message, String webhookType, String webhookData) {
        super(message);
        this.webhookType = webhookType;
        this.webhookData = webhookData;
    }
    
    public WebhookValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
