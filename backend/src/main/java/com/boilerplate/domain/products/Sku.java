package com.boilerplate.domain.products;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Catalog SKU value object. The format is enforced at construction so a raw string can
 * never masquerade as a well-formed SKU: three letters/digits, a dash, then at least four
 * more (e.g. {@code TSH-0042}, {@code MUG-ABCD}). Persisted as a plain {@code varchar}
 * column on {@code Product} — the aggregate wraps/unwraps it at its boundary.
 */
public record Sku(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9]{3}-[A-Z0-9]{4,}$");

    public Sku {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SKU must not be blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("\"" + value + "\" is not a valid SKU");
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
