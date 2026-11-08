package com.boilerplate.domain.products.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record ProductPriceChanged(
        UUID productId, long previousAmountMinorUnits, long newAmountMinorUnits, String currency)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "products.price_changed.v1";
    }

    @Override
    public UUID aggregateId() {
        return productId;
    }
}
