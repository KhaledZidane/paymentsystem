package com.codequest.service;

import com.codequest.exception.DuplicateActivePaymentException;
import com.codequest.exception.OrderNotFoundException;
import com.codequest.exception.PaymentNotFoundException;
import com.codequest.model.Order;
import com.codequest.model.OrderStatus;
import com.codequest.model.Payment;
import com.codequest.repository.OrderRepository;
import com.codequest.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Starts a new payment attempt for the given order.
     *
     * <p>Invariants enforced:
     * <ul>
     *   <li>Order must exist</li>
     *   <li>Order must allow a transition to PENDING_PAYMENT (i.e., CREATED or PAYMENT_FAILED)</li>
     *   <li>No active payment intent may already be in flight (prevents double charges)</li>
     * </ul>
     *
     * @return the newly created Payment (contains the paymentIntentId needed for webhook)
     */
    public synchronized Payment startPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Guard: no duplicate active payments
        if (order.hasActivePayment()) {
            throw new DuplicateActivePaymentException(orderId);
        }

        // Validates state machine: only CREATED or PAYMENT_FAILED → PENDING_PAYMENT
        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        Payment payment = new Payment(orderId);
        paymentRepository.save(payment);

        order.attachPaymentIntent(payment.getId());
        orderRepository.save(order);

        return payment;
    }

    /**
     * Processes an incoming payment webhook.
     *
     * <p>Idempotency: if the payment has already been processed (CONFIRMED or FAILED),
     * the call is a no-op — the same final state is returned without re-applying any transition.
     * This handles the case where the provider sends the same event more than once.
     *
     * @param paymentIntentId the payment identifier from the webhook
     * @param result          "CONFIRMED" or "FAILED"
     */
    public synchronized Order processWebhook(String paymentIntentId, String result) {
        Payment payment = paymentRepository.findById(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentIntentId));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));

        // Idempotency guard: if already processed, return current state unchanged
        if (payment.isProcessed()) {
            return order;
        }

        if ("CONFIRMED".equals(result)) {
            payment.confirm();
            order.transitionTo(OrderStatus.PAID);
        } else {
            payment.fail();
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
        }

        // Payment is no longer active — clear the reference so a retry is possible
        order.clearPaymentIntent();

        paymentRepository.save(payment);
        orderRepository.save(order);

        return order;
    }
}
