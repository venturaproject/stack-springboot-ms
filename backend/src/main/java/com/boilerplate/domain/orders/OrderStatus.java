package com.boilerplate.domain.orders;

/**
 * Lifecycle of an {@link Order}. Only {@code DRAFT} orders can be edited. {@code PLACED} is
 * the customer's intent recorded; the order becomes {@code CONFIRMED} once the fulfillment
 * saga has authorised payment and reserved stock, or {@code CANCELLED} if either step fails
 * (or the customer cancels while still in draft).
 */
public enum OrderStatus {
    DRAFT,
    PLACED,
    CONFIRMED,
    CANCELLED
}
