package com.boilerplate.application.customers;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Register a customer. The four address fields are all-or-nothing: supply every one to
 * set an address, or leave them all null.
 */
public record RegisterCustomerCommand(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @Nullable @Size(max = 255) String addressStreet,
        @Nullable @Size(max = 255) String addressCity,
        @Nullable @Size(max = 32) String addressPostalCode,
        @Nullable @Size(max = 255) String addressCountry) {}
