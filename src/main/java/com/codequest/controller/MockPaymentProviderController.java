package com.codequest.controller;

import com.codequest.model.Order;
import com.codequest.service.PaymentService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock/payment")
public class MockPaymentProviderController {

    private final PaymentService paymentService;

    public MockPaymentProviderController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Simulates the provider confirming payment and triggering the webhook.
     */
    @PostMapping("/{paymentIntentId}/confirm")
    public Order simulateConfirmed(@PathVariable String paymentIntentId) {
        return paymentService.processWebhook(paymentIntentId, "CONFIRMED");
    }

    /**
     * Simulates the provider reporting a payment failure and triggering the webhook.
     */
    @PostMapping("/{paymentIntentId}/fail")
    public Order simulateFailed(@PathVariable String paymentIntentId) {
        return paymentService.processWebhook(paymentIntentId, "FAILED");
    }
}
