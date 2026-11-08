package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HashedPasswordTest {

    @Test
    void accepts_a_hash_looking_string() {
        assertThat(new HashedPassword("$argon2id$v=19$m=16,t=2,p=1$abc$def").value())
                .startsWith("$argon2id");
    }

    @Test
    void rejects_plaintext() {
        assertThatThrownBy(() -> new HashedPassword("hunter2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plaintext");
    }

    @Test
    void rejects_blank() {
        assertThatThrownBy(() -> new HashedPassword(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toString_does_not_leak_the_hash() {
        assertThat(new HashedPassword("$2a$10$secret").toString()).doesNotContain("secret");
    }
}
