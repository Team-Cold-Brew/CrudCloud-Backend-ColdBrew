package com.riwi.CrudCloud.mercadoPago.util.exception.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment error responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentErrorResponse {

    private String code;
    private String message;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    private String path;

    public PaymentErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public PaymentErrorResponse(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}
