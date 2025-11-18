package com.riwi.CrudCloud.mercadoPago.dto.response;

import java.time.LocalDateTime;

import com.riwi.CrudCloud.common.models.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for subscription information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private Long subscriptionId;
    private Integer userId;
    private Integer planId;
    private String planName;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;
    private Boolean autoRenewal;
    private LocalDateTime createdAt;
}
