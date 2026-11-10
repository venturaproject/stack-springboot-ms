package com.boilerplate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryLoginRateLimiterTest {

    @Test
    void consumes_the_configured_burst_then_rejects_until_refill() {
        InMemoryLoginRateLimiter limiter = new InMemoryLoginRateLimiter();

        assertThat(limiter.tryAcquire("login|192.0.2.1", 1, 1)).isTrue();
        assertThat(limiter.tryAcquire("login|192.0.2.1", 1, 1)).isFalse();
    }
}
