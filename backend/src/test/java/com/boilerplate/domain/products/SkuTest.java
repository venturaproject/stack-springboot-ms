package com.boilerplate.domain.products;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SkuTest {

    @Test
    void normalises_to_upper_case_and_trims() {
        assertThat(new Sku("  tsh-0042 ").value()).isEqualTo("TSH-0042");
    }

    @Test
    void rejects_a_malformed_sku() {
        assertThatThrownBy(() -> new Sku("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Sku("TS-0042")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Sku("TSH-01")).isInstanceOf(IllegalArgumentException.class);
    }
}
