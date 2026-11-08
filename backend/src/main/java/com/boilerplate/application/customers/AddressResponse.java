package com.boilerplate.application.customers;

import com.boilerplate.domain.customers.Address;
import jakarta.annotation.Nullable;

public record AddressResponse(String street, String city, String postalCode, String country) {

    @Nullable
    public static AddressResponse from(@Nullable Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(address.street(), address.city(), address.postalCode(), address.country());
    }
}
