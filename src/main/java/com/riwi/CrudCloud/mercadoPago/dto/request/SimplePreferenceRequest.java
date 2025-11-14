package com.riwi.CrudCloud.mercadoPago.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SimplePreferenceRequest {
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency = "ARS";
}
