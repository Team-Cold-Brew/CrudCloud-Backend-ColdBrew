package com.riwi.CrudCloud.mercadoPago.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for checkout preference creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {

    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private String externalReference;
    private String message;
}
