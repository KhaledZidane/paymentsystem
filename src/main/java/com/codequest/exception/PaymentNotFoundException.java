package com.codequest.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String paymentIntentId) {
        super("Payment intent not found: " + paymentIntentId);
    }
}
