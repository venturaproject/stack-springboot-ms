package com.boilerplate.application.products;

import com.boilerplate.domain.products.Product;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        @Nullable String description,
        long priceAmountMinorUnits,
        String priceCurrency,
        int stockQuantity,
        @Nullable String imageUrl,
        Instant createdAt) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku().value(),
                p.getName(),
                p.getDescription(),
                p.getPrice().amountMinorUnits(),
                p.getPrice().currency().name(),
                p.getStockQuantity(),
                p.getImageFilename() == null ? null : "/api" + p.getImageFilename(),
                p.getCreatedAt());
    }
}
