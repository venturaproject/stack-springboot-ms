package com.boilerplate.domain.orders.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record OrderLineAdded(UUID orderId, UUID productId, int quantity) implements DomainEvent {

    @Override
    public String eventType() {
        return "orders.line_added.v1";
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
