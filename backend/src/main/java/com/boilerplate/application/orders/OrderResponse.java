package com.boilerplate.application.orders;

import com.boilerplate.application.customers.AddressResponse;
import com.boilerplate.domain.orders.Order;
import com.boilerplate.domain.shared.Money;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full order view, lines included. */
public record OrderResponse(
        UUID id,
        UUID customerId,
        String status,
        @Nullable AddressResponse shippingAddress,
        @Nullable Long totalAmountMinorUnits,
        @Nullable String currency,
        List<OrderLineResponse> lines,
        Instant createdAt) {
    public static OrderResponse from(Order o) {
        Money total = o.getTotal();
        return new OrderResponse(
                o.getId(),
                o.getCustomerId(),
                o.getStatus().name(),
                AddressResponse.from(o.getShippingAddress()),
                total == null ? null : total.amountMinorUnits(),
                total == null ? null : total.currency().name(),
                o.getLines().stream().map(OrderLineResponse::from).toList(),
                o.getCreatedAt());
    }
}
