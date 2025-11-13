package com.riwi.CrudCloud.mercadoPago.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreferenceRequest {

    @NotNull(message = "Plan ID is required")
    private Integer planId;

    @NotNull(message = "User ID is required")
    private Integer userId;

    private Integer organizationId; // Optional for organization payments

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "ARS"; // Default to Argentine Peso

    private String description;
}
