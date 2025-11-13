package com.riwi.CrudCloud.mercadoPago.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Checkout Pro payment creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutProResponse {

    private String preferenceId;
    private String publicKey;
    private Integer transactionId;
    private String status;
    private String message;

    public CheckoutProResponse(String preferenceId, String publicKey, Integer transactionId) {
        this.preferenceId = preferenceId;
        this.publicKey = publicKey;
        this.transactionId = transactionId;
        this.status = "created";
        this.message = "Preference created successfully for Checkout Pro";
    }
}
