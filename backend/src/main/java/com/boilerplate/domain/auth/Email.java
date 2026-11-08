package com.boilerplate.domain.auth;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email address value object. A {@code User} can never hold a malformed address regardless
 * of how it was constructed (service, seeder, console command) — validation lives here,
 * not only at the DTO edge.
 */
public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
