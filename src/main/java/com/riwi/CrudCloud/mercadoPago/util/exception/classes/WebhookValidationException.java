package com.riwi.CrudCloud.mercadoPago.util.exception.classes;

/**
 * Exception thrown when webhook validation fails
 */
public class WebhookValidationException extends PaymentException {

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

    public String getWebhookType() {
        return webhookType;
    }

    public String getWebhookData() {
        return webhookData;
    }
}
