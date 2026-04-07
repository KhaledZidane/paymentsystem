package com.codequest.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class Payment {

    private final String id;
    private final String orderId;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant processedAt;

    public Payment(String orderId) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void confirm() {
        this.status = PaymentStatus.CONFIRMED;
        this.processedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.processedAt = Instant.now();
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isProcessed() {
        return status == PaymentStatus.CONFIRMED || status == PaymentStatus.FAILED;
    }
}
