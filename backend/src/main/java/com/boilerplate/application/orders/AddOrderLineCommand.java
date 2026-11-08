package com.boilerplate.application.orders;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Append a line to an order. Pass {@code orderId} to add to an existing draft, or
 * {@code customerId} to start a new draft for that customer and add the first line —
 * the "add to cart" flow where the cart is created implicitly on first add.
 */
public record AddOrderLineCommand(
        @Nullable UUID orderId, @Nullable UUID customerId, @NotNull UUID productId, @Positive int quantity) {}
