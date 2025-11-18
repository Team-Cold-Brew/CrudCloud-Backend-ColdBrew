package com.riwi.CrudCloud.mercadoPago.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MercadoPago Webhook notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRequest {

    private String action;
    private String apiVersion;
    private WebhookData data;
    private String dateCreated;
    private Long id;
    private Boolean liveMode;
    private String type;
    private String userId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebhookData {
        private String id;
    }
}
