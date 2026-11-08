package com.boilerplate.domain.products.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record StockAdjusted(UUID productId, int delta, int newQuantity, String reason) implements DomainEvent {

    @Override
    public String eventType() {
        return "products.stock_adjusted.v1";
    }

    @Override
    public UUID aggregateId() {
        return productId;
    }
}
