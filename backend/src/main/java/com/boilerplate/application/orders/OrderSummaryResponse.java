package com.boilerplate.application.orders;

import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.shared.Money;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/** Lightweight order view for list endpoints — no line collection. */
public record OrderSummaryResponse(
        UUID id,
        UUID customerId,
        String status,
        @Nullable Long totalAmountMinorUnits,
        @Nullable String currency,
        Instant createdAt) {
    public static OrderSummaryResponse from(Order o) {
        Money total = o.getTotal();
        return new OrderSummaryResponse(
                o.getId(),
                o.getCustomerId(),
                o.getStatus().name(),
                total == null ? null : total.amountMinorUnits(),
                total == null ? null : total.currency().name(),
                o.getCreatedAt());
    }
}
