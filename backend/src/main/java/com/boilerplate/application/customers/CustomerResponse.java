package com.boilerplate.application.customers;

import com.boilerplate.domain.customers.Customer;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/** Full customer view for detail endpoints. */
public record CustomerResponse(
        UUID id, String name, String email, @Nullable AddressResponse address, Instant createdAt) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getEmail().value(), AddressResponse.from(c.getAddress()), c.getCreatedAt());
    }
}
