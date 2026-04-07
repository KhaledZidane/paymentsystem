package com.codequest.controller;

import com.codequest.model.Order;
import com.codequest.model.Payment;
import com.codequest.service.OrderService;
import com.codequest.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    /**
     * GET /orders/{orderId}
     * Returns the current state of the order.
     */
    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    /**
     * POST /orders/{orderId}/payment/start
     * Initiates a payment attempt. Moves order to PENDING_PAYMENT.
     * Returns 409 if an active payment is already in flight, or an invalid state transition is attempted.
     */
    @PostMapping("/{orderId}/payment/start")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment startPayment(@PathVariable String orderId) {
        return paymentService.startPayment(orderId);
    }

    /**
     * POST /orders/{orderId}/cancel
     * Cancels the order. Only valid from CREATED or PAYMENT_FAILED.
     * Returns 409 if the order is in PENDING_PAYMENT or already terminal (PAID, CANCELLED).
     */
    @PostMapping("/{orderId}/cancel")
    public Order cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }
}
