package com.boilerplate.domain.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiClientTokenTest {

    @Test
    void expiry_windows_are_derived_from_ttls() {
        Instant now = Instant.now().minusSeconds(10);
        ApiClientToken fresh = ApiClientToken.issue("cli", "a", "r", List.of("s"), now, 3600, 7200);
        assertThat(fresh.isAccessExpired()).isFalse();
        assertThat(fresh.isRefreshExpired()).isFalse();

        ApiClientToken accessGone = ApiClientToken.issue("cli", "a", "r", List.of("s"), now, 5, 7200);
        assertThat(accessGone.isAccessExpired()).isTrue();
        assertThat(accessGone.isRefreshExpired()).isFalse();

        ApiClientToken bothGone = ApiClientToken.issue("cli", "a", "r", List.of("s"), now, 1, 2);
        assertThat(bothGone.isRefreshExpired()).isTrue();
    }
}
