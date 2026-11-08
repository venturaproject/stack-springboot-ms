package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private User user() {
        return User.register(new Email("a@b.com"), new HashedPassword("$stub"), "A", "B", Set.of());
    }

    private RefreshToken fresh() {
        return RefreshToken.startFamily("tok", user(), Instant.now().plusSeconds(60));
    }

    @Test
    void a_fresh_token_is_valid() {
        RefreshToken t = fresh();
        assertThat(t.isValid()).isTrue();
        assertThat(t.isExpired()).isFalse();
        assertThat(t.isRevoked()).isFalse();
        assertThat(t.getFamilyId()).isNotNull();
    }

    @Test
    void an_expired_token_is_invalid() {
        RefreshToken t = RefreshToken.startFamily("tok", user(), Instant.now().minusSeconds(1));
        assertThat(t.isExpired()).isTrue();
        assertThat(t.isValid()).isFalse();
    }

    @Test
    void a_revoked_token_is_invalid() {
        RefreshToken t = fresh();
        t.revoke();
        assertThat(t.isValid()).isFalse();
    }

    @Test
    void rotate_keeps_the_same_family() {
        RefreshToken first = fresh();
        RefreshToken next = first.rotate("tok2", Instant.now().plusSeconds(60));
        assertThat(next.getFamilyId()).isEqualTo(first.getFamilyId());
        assertThat(next.getToken()).isEqualTo("tok2");
        assertThat(next.isRevoked()).isFalse();
    }

    @Test
    void startFamily_requires_token_and_user() {
        assertThatThrownBy(() -> RefreshToken.startFamily(" ", user(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RefreshToken.startFamily("tok", null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
