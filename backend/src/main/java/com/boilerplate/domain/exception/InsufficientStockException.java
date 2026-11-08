package com.boilerplate.domain.exception;

/** A stock reservation asked for more units than a product has on hand. Maps to HTTP 422. */
public class InsufficientStockException extends BusinessRuleViolationException {

    public InsufficientStockException(int requested, int available) {
        super("Insufficient stock: requested " + requested + ", only " + available + " available");
    }
}
