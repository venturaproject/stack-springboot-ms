package com.boilerplate.domain.auth;

/**
 * A password that has already been hashed. Wrapping the hash in a type makes it a
 * compile-time error to store a plaintext password on a {@link User}, and gives the
 * domain a single place that knows what a valid hash looks like.
 */
public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        if (!value.startsWith("$")) {
            throw new IllegalArgumentException(
                    "Password hash does not look like a hash — refusing to store a plaintext password");
        }
    }

    public static HashedPassword ofHash(String hash) {
        return new HashedPassword(hash);
    }

    @Override
    public String toString() {
        return "HashedPassword[***]";
    }
}
