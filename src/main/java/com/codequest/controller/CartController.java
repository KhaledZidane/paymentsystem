package com.codequest.controller;

import com.codequest.dto.AddItemRequest;
import com.codequest.model.Cart;
import com.codequest.model.Order;
import com.codequest.service.CartService;
import com.codequest.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;

    public CartController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    /**
     * POST /carts
     * Creates a new empty cart.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cart createCart() {
        return cartService.createCart();
    }

    /**
     * GET /carts/{cartId}
     * Returns the current cart state.
     */
    @GetMapping("/{cartId}")
    public Cart getCart(@PathVariable String cartId) {
        return cartService.getCart(cartId);
    }

    /**
     * POST /carts/{cartId}/items
     * Adds a product to the cart. Merges quantity for duplicate products.
     * Returns 409 if the cart is already checked out.
     */
    @PostMapping("/{cartId}/items")
    public Cart addItem(
            @PathVariable String cartId,
            @Valid @RequestBody AddItemRequest request
    ) {
        return cartService.addItem(cartId, request.productId(), request.quantity(), request.price());
    }

    /**
     * POST /carts/{cartId}/checkout
     * Locks the cart and creates an Order in CREATED state.
     * Returns 409 if already checked out, 422 if the cart is empty.
     */
    @PostMapping("/{cartId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public Order checkout(@PathVariable String cartId) {
        return orderService.checkout(cartId);
    }
}