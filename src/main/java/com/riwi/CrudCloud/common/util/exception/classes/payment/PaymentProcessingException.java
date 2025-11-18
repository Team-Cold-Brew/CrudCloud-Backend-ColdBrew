package com.riwi.CrudCloud.common.util.exception.classes.payment;

/**
 * Exception thrown when there are errors processing payments
 */
public class PaymentProcessingException extends PaymentException {

    private String transactionId;
    private String paymentStatus;

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentProcessingException(String message, String transactionId, String paymentStatus) {
        super(message);
        this.transactionId = transactionId;
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }
}
