package com.boilerplate.domain.customers.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record CustomerAddressChanged(UUID customerId) implements DomainEvent {

    @Override
    public String eventType() {
        return "customers.address_changed.v1";
    }

    @Override
    public UUID aggregateId() {
        return customerId;
    }
}
