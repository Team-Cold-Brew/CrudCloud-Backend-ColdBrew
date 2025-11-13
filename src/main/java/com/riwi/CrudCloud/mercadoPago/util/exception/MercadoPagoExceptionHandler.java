package com.riwi.CrudCloud.mercadoPago.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.riwi.CrudCloud.mercadoPago.util.exception.classes.PaymentException;
import com.riwi.CrudCloud.mercadoPago.util.exception.dto.PaymentErrorResponse;

/**
 * Global exception handler for all payment-related exceptions
 * This handler catches any PaymentException that wasn't handled by specific handlers
 */
@ControllerAdvice(basePackages = "com.riwi.CrudCloud.mercadoPago")
public class MercadoPagoExceptionHandler {

    /**
     * Handle generic payment exceptions
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<PaymentErrorResponse> handlePaymentException(
            PaymentException ex, WebRequest request) {
        
        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "PAYMENT_ERROR",
            ex.getMessage()
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle unexpected exceptions in payment module
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "INTERNAL_PAYMENT_ERROR",
            "An unexpected error occurred while processing payment: " + ex.getMessage()
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
