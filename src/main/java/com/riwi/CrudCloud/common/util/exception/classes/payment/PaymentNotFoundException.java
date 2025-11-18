package com.riwi.CrudCloud.common.util.exception.classes.payment;

/**
 * Exception thrown when a payment or transaction is not found
 */
public class PaymentNotFoundException extends PaymentException {

    private String transactionId;
    private String paymentId;

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public PaymentNotFoundException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public PaymentNotFoundException(String message, String transactionId, String paymentId) {
        super(message);
        this.transactionId = transactionId;
        this.paymentId = paymentId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}
