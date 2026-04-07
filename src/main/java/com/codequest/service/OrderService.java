package com.codequest.service;

import com.codequest.exception.CartAlreadyCheckedOutException;
import com.codequest.exception.CartNotFoundException;
import com.codequest.exception.InvalidStateTransitionException;
import com.codequest.exception.OrderNotFoundException;
import com.codequest.model.Cart;
import com.codequest.model.Order;
import com.codequest.model.OrderStatus;
import com.codequest.repository.CartRepository;
import com.codequest.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public OrderService(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Creates an order from the cart and locks it so no further modifications are possible.
     * Invariant: cart must be OPEN and non-empty.
     */
    public Order checkout(String cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));

        if (!cart.isOpen()) {
            throw new CartAlreadyCheckedOutException(cartId);
        }

        // Locks the cart — throws IllegalStateException if empty
        cart.checkout();
        cartRepository.save(cart);

        Order order = new Order(cartId, cart.getItems(), cart.total());
        return orderRepository.save(order);
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    /**
     * Cancels the order.
     *
     * <p>Valid from CREATED or PAYMENT_FAILED only.
     * Cancelling while PENDING_PAYMENT is rejected because a payment is in-flight.
     * PAID and CANCELLED are terminal — cancellation is not possible.
     *
     * @throws InvalidStateTransitionException mapped to 409 by GlobalExceptionHandler
     */
    public synchronized Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.transitionTo(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}
