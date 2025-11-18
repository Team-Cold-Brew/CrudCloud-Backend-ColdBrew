package com.riwi.CrudCloud.mercadoPago.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a checkout preference
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Plan ID is required")
    private Integer planId;

    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private String externalReference;
}
