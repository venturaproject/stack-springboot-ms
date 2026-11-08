package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void normalises_to_trimmed_lowercase() {
        assertThat(new Email("  Admin@Example.COM ").value()).isEqualTo("admin@example.com");
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new Email("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_malformed() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("a@b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("a b@c.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_over_254_chars() {
        String local = "a".repeat(250);
        assertThatThrownBy(() -> new Email(local + "@x.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equality_is_by_normalised_value() {
        assertThat(new Email("USER@x.com")).isEqualTo(new Email("user@x.com"));
    }
}
