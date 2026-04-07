package com.codequest.service;

import com.codequest.exception.CartAlreadyCheckedOutException;
import com.codequest.exception.CartNotFoundException;
import com.codequest.model.Cart;
import com.codequest.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Cart createCart() {
        Cart cart = new Cart();
        return cartRepository.save(cart);
    }

    public Cart getCart(String cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId));
    }

    /**
     * Adds an item to the cart. Merges quantity if the product already exists.
     * Rejects the operation if the cart is no longer open.
     */
    public Cart addItem(String cartId, String productId, int quantity, BigDecimal price) {
        Cart cart = getCart(cartId);
        if (!cart.isOpen()) {
            throw new CartAlreadyCheckedOutException(cartId);
        }
        cart.addItem(productId, quantity, price);
        return cartRepository.save(cart);
    }
}
