package com.codequest.exception;

public class CartAlreadyCheckedOutException extends RuntimeException {
    public CartAlreadyCheckedOutException(String cartId) {
        super("Cart has already been checked out: " + cartId);
    }
}
