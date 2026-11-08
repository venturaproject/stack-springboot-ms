package com.boilerplate.application.customers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerAddressCommand(
        @NotBlank @Size(max = 255) String street,
        @NotBlank @Size(max = 255) String city,
        @NotBlank @Size(max = 32) String postalCode,
        @NotBlank @Size(max = 255) String country) {}
