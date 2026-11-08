package com.boilerplate.domain.products.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record ProductCreated(UUID productId, String sku, String name) implements DomainEvent {

    @Override
    public String eventType() {
        return "products.created.v1";
    }

    @Override
    public UUID aggregateId() {
        return productId;
    }
}
