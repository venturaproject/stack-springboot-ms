package com.boilerplate.application.products;

import com.boilerplate.application.dispatch.Command;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductCommand(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @Nullable @Size(max = 5000) String description,
        @PositiveOrZero long priceAmountMinorUnits,
        @NotBlank @Size(min = 3, max = 3) String priceCurrency,
        @PositiveOrZero int initialStock)
        implements Command<ProductResponse> {}
