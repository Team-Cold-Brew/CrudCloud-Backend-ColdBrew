package com.riwi.CrudCloud.mercadoPago.util.exception.handlers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.riwi.CrudCloud.mercadoPago.util.exception.classes.InvalidPaymentDataException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.MercadoPagoException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.PaymentNotFoundException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.PaymentProcessingException;
import com.riwi.CrudCloud.mercadoPago.util.exception.classes.WebhookValidationException;
import com.riwi.CrudCloud.mercadoPago.util.exception.dto.PaymentErrorResponse;

/**
 * Global exception handler for payment-related exceptions
 */
@ControllerAdvice
public class PaymentExceptionHandler {

    /**
     * Handle MercadoPago API exceptions
     */
    @ExceptionHandler(MercadoPagoException.class)
    public ResponseEntity<PaymentErrorResponse> handleMercadoPagoException(
            MercadoPagoException ex, WebRequest request) {
        
        Map<String, Object> details = Map.of(
            "mercadoPagoErrorCode", ex.getMercadoPagoErrorCode() != null ? ex.getMercadoPagoErrorCode() : "UNKNOWN",
            "mercadoPagoErrorMessage", ex.getMercadoPagoErrorMessage() != null ? ex.getMercadoPagoErrorMessage() : "No details"
        );

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "MERCADO_PAGO_ERROR",
            ex.getMessage(),
            details
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handle payment processing exceptions
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<PaymentErrorResponse> handlePaymentProcessingException(
            PaymentProcessingException ex, WebRequest request) {
        
        Map<String, Object> details = Map.of(
            "transactionId", ex.getTransactionId() != null ? ex.getTransactionId() : "UNKNOWN",
            "paymentStatus", ex.getPaymentStatus() != null ? ex.getPaymentStatus() : "UNKNOWN"
        );

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "PAYMENT_PROCESSING_ERROR",
            ex.getMessage(),
            details
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle invalid payment data exceptions
     */
    @ExceptionHandler(InvalidPaymentDataException.class)
    public ResponseEntity<PaymentErrorResponse> handleInvalidPaymentDataException(
            InvalidPaymentDataException ex, WebRequest request) {
        
        Map<String, Object> details = Map.of(
            "fieldName", ex.getFieldName() != null ? ex.getFieldName() : "UNKNOWN",
            "fieldValue", ex.getFieldValue() != null ? ex.getFieldValue().toString() : "NULL"
        );

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "INVALID_PAYMENT_DATA",
            ex.getMessage(),
            details
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle payment not found exceptions
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<PaymentErrorResponse> handlePaymentNotFoundException(
            PaymentNotFoundException ex, WebRequest request) {
        
        Map<String, Object> details = Map.of(
            "transactionId", ex.getTransactionId() != null ? ex.getTransactionId() : "UNKNOWN",
            "paymentId", ex.getPaymentId() != null ? ex.getPaymentId() : "UNKNOWN"
        );

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "PAYMENT_NOT_FOUND",
            ex.getMessage(),
            details
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle webhook validation exceptions
     */
    @ExceptionHandler(WebhookValidationException.class)
    public ResponseEntity<PaymentErrorResponse> handleWebhookValidationException(
            WebhookValidationException ex, WebRequest request) {
        
        Map<String, Object> details = Map.of(
            "webhookType", ex.getWebhookType() != null ? ex.getWebhookType() : "UNKNOWN",
            "webhookData", ex.getWebhookData() != null ? ex.getWebhookData() : "NO_DATA"
        );

        PaymentErrorResponse errorResponse = new PaymentErrorResponse(
            "WEBHOOK_VALIDATION_ERROR",
            ex.getMessage(),
            details
        );
        errorResponse.setPath(request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
