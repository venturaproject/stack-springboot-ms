package com.boilerplate.domain.orders.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record OrderCancelled(UUID orderId) implements DomainEvent {

    @Override
    public String eventType() {
        return "orders.cancelled.v1";
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
