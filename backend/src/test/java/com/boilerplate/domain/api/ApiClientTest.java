package com.boilerplate.domain.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiClientTest {

    private ApiClient issue() {
        return ApiClient.issue("CI bot", "cli_abc", "hash", List.of("products:read"), Instant.now());
    }

    @Test
    void issued_client_is_active() {
        assertThat(issue().isActive()).isTrue();
    }

    @Test
    void issue_requires_name_and_scopes() {
        assertThatThrownBy(() -> ApiClient.issue(" ", "c", "h", List.of("x"), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiClient.issue("n", "c", "h", List.of(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revoke_deactivates() {
        ApiClient c = issue();
        c.revoke();
        assertThat(c.isActive()).isFalse();
    }

    @Test
    void secretMatches_compares_hashes() {
        ApiClient c = issue();
        assertThat(c.secretMatches("hash")).isTrue();
        assertThat(c.secretMatches("nope")).isFalse();
    }

    @Test
    void recordUsage_sets_last_used_at() {
        ApiClient c = issue();
        Instant when = Instant.now();
        c.recordUsage(when);
        assertThat(c.getLastUsedAt()).isEqualTo(when);
    }

    @Test
    void scopes_are_copied_defensively() {
        var scopes = new java.util.ArrayList<>(List.of("products:read"));
        ApiClient c = ApiClient.issue("n", "c", "h", scopes, Instant.now());
        scopes.clear();
        assertThat(c.getScopes()).containsExactly("products:read");
    }
}
