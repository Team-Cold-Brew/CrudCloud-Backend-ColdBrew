package com.riwi.CrudCloud.mercadoPago.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.riwi.CrudCloud.common.models.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;
    private String mercadopagoPaymentId;
    private Integer userId;
    private Integer planId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String statusDetail;
    private String paymentMethod;
    private String paymentType;
    private String description;
    private String externalReference;
    private String payerEmail;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
