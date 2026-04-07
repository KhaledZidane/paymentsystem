package com.codequest.model;


import com.codequest.exception.InvalidStateTransitionException;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    PAID,
    CANCELLED;

    /**
     * Validates that a transition from this state to {@code next} is permitted.
     * Throws {@link InvalidStateTransitionException} if the transition is illegal.
     */
    public void validateTransitionTo(OrderStatus next) {
        boolean valid = switch (this) {
            case CREATED, PAYMENT_FAILED -> next == PENDING_PAYMENT || next == CANCELLED;
            case PENDING_PAYMENT -> next == PAID || next == PAYMENT_FAILED;
            case PAID            -> false; // terminal state
            case CANCELLED       -> false; // terminal state
        };
        if (!valid) {
            throw new InvalidStateTransitionException(this, next);
        }
    }
}