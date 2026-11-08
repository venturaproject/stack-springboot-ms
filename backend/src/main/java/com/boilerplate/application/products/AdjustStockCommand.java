package com.boilerplate.application.products;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

/**
 * Adjust stock by a signed delta: positive restocks, negative reserves. Zero is rejected
 * by the aggregate.
 */
public record AdjustStockCommand(int delta, @Nullable @Size(max = 100) String reason) {
    public String reasonOrDefault() {
        return reason == null || reason.isBlank() ? "manual_adjustment" : reason.trim();
    }
}
