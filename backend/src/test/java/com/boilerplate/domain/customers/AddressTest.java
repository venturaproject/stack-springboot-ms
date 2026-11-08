package com.boilerplate.domain.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AddressTest {

    @Test
    void trims_every_field() {
        Address address = new Address(" 1 High St ", " Oxford ", " OX1 ", " UK ");
        assertThat(address.street()).isEqualTo("1 High St");
        assertThat(address.city()).isEqualTo("Oxford");
    }

    @Test
    void rejects_any_blank_field() {
        assertThatThrownBy(() -> new Address("street", "city", "postal", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
