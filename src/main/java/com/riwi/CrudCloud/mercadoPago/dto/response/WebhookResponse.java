package com.riwi.CrudCloud.mercadoPago.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private String status;
    private String message;
    private String transactionId;
    private String paymentStatus;

    public static WebhookResponse success(String transactionId, String paymentStatus) {
        return new WebhookResponse("success", "Webhook processed successfully", transactionId, paymentStatus);
    }

    public static WebhookResponse error(String message) {
        return new WebhookResponse("error", message, null, null);
    }
}
