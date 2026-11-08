package com.boilerplate.domain.shared;

/** Supported settlement currencies. The string is the ISO-4217 code and the wire value. */
public enum Currency {
    USD,
    EUR;

    public static Currency of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code must not be blank");
        }
        try {
            return Currency.valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported currency: " + code);
        }
    }
}
