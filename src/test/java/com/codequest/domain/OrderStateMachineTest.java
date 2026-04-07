package com.codequest.domain;

import com.codequest.exception.InvalidStateTransitionException;
import com.codequest.model.CartItem;
import com.codequest.model.Order;
import com.codequest.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Order state machine.
 */
class OrderStateMachineTest {

    private Order newOrder() {
        CartItem item = new CartItem("product-1", 2, new BigDecimal("9.99"));
        return new Order("cart-1", List.of(item), new BigDecimal("19.98"));
    }

    @Test
    void created_canTransitionTo_pendingPayment() {
        Order order = newOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void pendingPayment_canTransitionTo_paid() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        order.transitionTo(OrderStatus.PAID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void pendingPayment_canTransitionTo_paymentFailed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        order.transitionTo(OrderStatus.PAYMENT_FAILED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void paymentFailed_canTransitionTo_pendingPayment() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order.transitionTo(OrderStatus.PAYMENT_FAILED);

        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void created_cannotTransitionTo_paid() {
        Order order = newOrder();

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("CREATED")
                .hasMessageContaining("PAID");
    }

    @Test
    void created_cannotTransitionTo_paymentFailed() {
        Order order = newOrder();

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAYMENT_FAILED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void paid_isTerminal_cannotTransitionToAnyState() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order.transitionTo(OrderStatus.PAID);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDING_PAYMENT))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("PAID");

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAYMENT_FAILED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void paymentFailed_cannotTransitionTo_paid() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order.transitionTo(OrderStatus.PAYMENT_FAILED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void order_totalMatchesCartItems() {
        CartItem a = new CartItem("product-a", 3, new BigDecimal("10.00"));
        CartItem b = new CartItem("product-b", 1, new BigDecimal("5.50"));
        Order order = new Order("cart-x", List.of(a, b), new BigDecimal("35.50"));

        assertThat(order.getTotal()).isEqualByComparingTo("35.50");
    }

    @Test
    void order_initiallyHasNoActivePaymentIntent() {
        Order order = newOrder();
        assertThat(order.hasActivePayment()).isFalse();
        assertThat(order.getActivePaymentIntentId()).isNull();
    }

    @Test
    void order_attachAndClearPaymentIntent() {
        Order order = newOrder();

        order.attachPaymentIntent("pay-abc");
        assertThat(order.hasActivePayment()).isTrue();
        assertThat(order.getActivePaymentIntentId()).isEqualTo("pay-abc");

        order.clearPaymentIntent();
        assertThat(order.hasActivePayment()).isFalse();
        assertThat(order.getActivePaymentIntentId()).isNull();
    }

    @Test
    void created_canTransitionTo_cancelled() {
        Order order = newOrder();

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void paymentFailed_canTransitionTo_cancelled() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order.transitionTo(OrderStatus.PAYMENT_FAILED);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void pendingPayment_cannotTransitionTo_cancelled() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("PENDING_PAYMENT")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void paid_cannotTransitionTo_cancelled() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        order.transitionTo(OrderStatus.PAID);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelled_isTerminal_cannotTransitionToAnyState() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CREATED))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDING_PAYMENT))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
