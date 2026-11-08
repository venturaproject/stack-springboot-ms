package com.boilerplate.application.orders;

import com.boilerplate.domain.orders.OrderLine;
import java.util.UUID;

public record OrderLineResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        long unitPriceAmountMinorUnits,
        String currency,
        int quantity,
        long lineTotalMinorUnits) {
    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getProductId(),
                line.getProductSku().value(),
                line.getProductName(),
                line.getUnitPrice().amountMinorUnits(),
                line.getUnitPrice().currency().name(),
                line.getQuantity(),
                line.getLineTotal().amountMinorUnits());
    }
}
