package com.boilerplate.domain.orders.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record OrderPlaced(UUID orderId, UUID customerId, long totalAmountMinorUnits, String currency)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "orders.placed.v1";
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}
