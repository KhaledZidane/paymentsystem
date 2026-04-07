package com.codequest.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class Cart {

    private final String id;
    private CartStatus status;
    private final List<CartItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    public Cart() {
        this.id = UUID.randomUUID().toString();
        this.status = CartStatus.OPEN;
        this.items = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == CartStatus.OPEN;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Adds or merges an item into the cart.
     * Throws if the cart is already checked out.
     */
    public void addItem(String productId, int quantity, BigDecimal price) {
        if (status != CartStatus.OPEN) {
            throw new IllegalStateException("Cart is already checked out and cannot be modified");
        }
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
        } else {
            items.add(new CartItem(productId, quantity, price));
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Locks the cart so no further modifications are possible.
     */
    public void checkout() {
        if (status != CartStatus.OPEN) {
            throw new IllegalStateException("Cart is already checked out");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = Instant.now();
    }

    public BigDecimal total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
