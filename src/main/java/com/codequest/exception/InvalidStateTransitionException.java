package com.codequest.exception;


import com.codequest.model.OrderStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order state transition: " + from + " → " + to);
    }
}
