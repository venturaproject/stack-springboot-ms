package com.boilerplate.application.customers;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Address;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Write use cases for the {@link Customer} aggregate. */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse register(RegisterCustomerCommand cmd) {
        Email email = new Email(cmd.email());
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateEntityException("A customer with that email already exists");
        }
        Customer customer = Customer.register(cmd.name(), email, addressOf(cmd));
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse updateAddress(UUID id, UpdateCustomerAddressCommand cmd) {
        Customer customer = load(id);
        customer.changeAddress(new Address(cmd.street(), cmd.city(), cmd.postalCode(), cmd.country()));
        return CustomerResponse.from(customerRepository.save(customer));
    }

    private Customer load(UUID id) {
        return customerRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    @Nullable
    private static Address addressOf(RegisterCustomerCommand cmd) {
        if (cmd.addressStreet() == null
                && cmd.addressCity() == null
                && cmd.addressPostalCode() == null
                && cmd.addressCountry() == null) {
            return null;
        }
        return new Address(
                required(cmd.addressStreet(), "street"),
                required(cmd.addressCity(), "city"),
                required(cmd.addressPostalCode(), "postal code"),
                required(cmd.addressCountry(), "country"));
    }

    private static String required(@Nullable String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Address " + field + " is required when any address field is set");
        }
        return value;
    }
}
