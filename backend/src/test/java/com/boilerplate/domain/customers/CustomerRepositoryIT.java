package com.boilerplate.domain.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.support.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CustomerRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void email_value_object_queries_and_uniqueness_check_work() {
        customerRepository.save(Customer.register("Alice", new Email("Alice@Example.com"), null));

        assertThat(customerRepository.existsByEmail(new Email("alice@example.com")))
                .isTrue();
        assertThat(customerRepository.findByEmail(new Email("alice@example.com")))
                .isPresent();
    }

    @Test
    void the_embedded_address_round_trips_and_filters() {
        Address address = new Address("221B Baker Street", "London", "NW1 6XE", "UK");
        customerRepository.save(Customer.register("WithAddr", new Email("addr@example.com"), address));
        customerRepository.save(Customer.register("NoAddr", new Email("noaddr@example.com"), null));

        var withAddress =
                customerRepository.search("", true, PageRequest.of(0, 50)).getContent();
        assertThat(withAddress).extracting(c -> c.getEmail().value()).contains("addr@example.com");
        assertThat(withAddress).extracting(c -> c.getEmail().value()).doesNotContain("noaddr@example.com");

        Customer reloaded =
                customerRepository.findByEmail(new Email("addr@example.com")).orElseThrow();
        assertThat(reloaded.getAddress()).isEqualTo(address);
    }
}
