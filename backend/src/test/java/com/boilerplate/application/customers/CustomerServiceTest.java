package com.boilerplate.application.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    CustomerService service;

    private static RegisterCustomerCommand cmd() {
        return new RegisterCustomerCommand("Alice", "alice@example.com", null, null, null, null);
    }

    @Test
    void register_persists_a_new_customer() {
        when(customerRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse res = service.register(cmd());

        assertThat(res.email()).isEqualTo("alice@example.com");
    }

    @Test
    void register_rejects_a_duplicate_email() {
        when(customerRepository.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(() -> service.register(cmd())).isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void updateAddress_on_a_missing_customer_is_a_404() {
        when(customerRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.updateAddress(UUID.randomUUID(), new UpdateCustomerAddressCommand("s", "c", "p", "co")))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
