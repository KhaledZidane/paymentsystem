package com.codequest.repository;

import com.codequest.model.Cart;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CartRepository {

    private final ConcurrentHashMap<String, Cart> store = new ConcurrentHashMap<>();

    public Cart save(Cart cart) {
        store.put(cart.getId(), cart);
        return cart;
    }

    public Optional<Cart> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
