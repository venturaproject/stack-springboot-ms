package com.boilerplate.application.products;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Partial update: a null field is left unchanged. Price is all-or-nothing (amount + currency). */
public record UpdateProductCommand(
        @Nullable @Size(max = 255) String name,
        @Nullable @Size(max = 5000) String description,
        @Nullable @PositiveOrZero Long priceAmountMinorUnits,
        @Nullable @Size(min = 3, max = 3) String priceCurrency) {}
