package com.codequest.controller;

import com.codequest.dto.WebhookRequest;
import com.codequest.model.Order;
import com.codequest.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /payments/webhook
     * Receives payment result events from the provider.
     *
     * <p>Idempotent: duplicate events for the same paymentIntentId are safely ignored.
     * The response always reflects the current order state.
     */
    @PostMapping("/webhook")
    public Order webhook(@Valid @RequestBody WebhookRequest request) {
        return paymentService.processWebhook(request.paymentIntentId(), request.result());
    }
}
