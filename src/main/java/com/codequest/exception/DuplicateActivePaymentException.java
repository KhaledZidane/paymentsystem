package com.codequest.exception;

public class DuplicateActivePaymentException extends RuntimeException {
    public DuplicateActivePaymentException(String orderId) {
        super("Order already has an active payment in flight: " + orderId);
    }
}
