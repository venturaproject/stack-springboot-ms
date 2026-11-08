package com.boilerplate.domain.customers.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record CustomerRegistered(UUID customerId, String email, String name) implements DomainEvent {

    @Override
    public String eventType() {
        return "customers.registered.v1";
    }

    @Override
    public UUID aggregateId() {
        return customerId;
    }
}
