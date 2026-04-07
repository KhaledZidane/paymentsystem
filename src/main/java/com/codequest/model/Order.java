package com.codequest.model;

import com.codequest.exception.InvalidStateTransitionException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {

    private final String id;
    private final String cartId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final BigDecimal total;

    /**
     * Tracks the ID of the currently active payment attempt.
     * Null when no payment is in flight. Set when a payment attempt is started,
     * cleared once the attempt reaches a terminal state (PAID or PAYMENT_FAILED).
     */
    private String activePaymentIntentId;

    private final Instant createdAt;
    private Instant updatedAt;

    public Order(String cartId, List<CartItem> cartItems, BigDecimal total) {
        this.id = UUID.randomUUID().toString();
        this.cartId = cartId;
        this.status = OrderStatus.CREATED;
        this.items = cartItems.stream()
                .map(ci -> new OrderItem(ci.getProductId(), ci.getQuantity(), ci.getPrice()))
                .collect(java.util.stream.Collectors.toList());
        this.total = total;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the order to a new status, enforcing the state machine.
     *
     * @throws InvalidStateTransitionException if the transition is illegal
     */
    public void transitionTo(OrderStatus next) {
        status.validateTransitionTo(next);
        this.status = next;
        this.updatedAt = Instant.now();
    }

    /**
     * Records that a new payment attempt has started.
     * Only allowed when the order is in a state that permits starting payment.
     */
    public void attachPaymentIntent(String paymentIntentId) {
        this.activePaymentIntentId = paymentIntentId;
        this.updatedAt = Instant.now();
    }

    /**
     * Clears the active payment intent once it has reached a terminal state.
     */
    public void clearPaymentIntent() {
        this.activePaymentIntentId = null;
        this.updatedAt = Instant.now();
    }

    public boolean hasActivePayment() {
        return activePaymentIntentId != null;
    }
}
