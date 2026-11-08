package com.boilerplate.domain.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.event.CustomerAddressChanged;
import com.boilerplate.domain.customers.event.CustomerRegistered;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final Address ADDRESS = new Address("221B Baker Street", "London", "NW1 6XE", "UK");

    @Test
    void register_records_a_CustomerRegistered_event_and_assigns_identity() {
        Customer customer = Customer.register("Alice", new Email("alice@example.com"), null);

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.hasAddress()).isFalse();
        assertThat(customer.recordedEvents()).singleElement().isInstanceOf(CustomerRegistered.class);
    }

    @Test
    void changeAddress_sets_it_and_records_an_event() {
        Customer customer = Customer.register("Alice", new Email("alice@example.com"), null);
        customer.clearEvents();

        customer.changeAddress(ADDRESS);

        assertThat(customer.getAddress()).isEqualTo(ADDRESS);
        assertThat(customer.recordedEvents()).singleElement().isInstanceOf(CustomerAddressChanged.class);
    }

    @Test
    void register_rejects_a_blank_name() {
        assertThatThrownBy(() -> Customer.register("  ", new Email("a@b.com"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
