package com.codequest.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartItem {

    private final String productId;
    private int quantity;
    private final BigDecimal price;

    public void addQuantity(int extra) {
        this.quantity += extra;
    }

    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
