package com.boilerplate.domain.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Immutable postal address value object. Copied by value onto an {@code Order} as a
 * shipping snapshot when a draft starts, so a later change to the customer's address
 * never rewrites historical orders. Mapped as a JPA {@code @Embeddable} — a {@code null}
 * embedded address means "no address on file".
 */
@Embeddable
public record Address(
        @Column(name = "address_street") String street,
        @Column(name = "address_city") String city,
        @Column(name = "address_postal_code") String postalCode,
        @Column(name = "address_country") String country) {

    public Address {
        street = requireField(street, "street");
        city = requireField(city, "city");
        postalCode = requireField(postalCode, "postal code");
        country = requireField(country, "country");
    }

    private static String requireField(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Address " + name + " must not be blank");
        }
        return value.trim();
    }
}
