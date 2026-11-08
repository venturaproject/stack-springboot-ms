package com.boilerplate.domain.orders.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

/** The fulfillment saga authorised payment and reserved stock — the order is going ahead. */
public record OrderConfirmed(UUID orderId, UUID customerId) implements DomainEvent {

    @Override
    public String eventType() {
        return "orders.confirmed.v1";
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
