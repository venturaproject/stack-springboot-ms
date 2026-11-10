package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.auth.event.RefreshTokenReused;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Email EMAIL = new Email("jane@example.com");
    private static final HashedPassword HASH = new HashedPassword("$argon2id$stub");

    private User newUser() {
        return User.register(EMAIL, HASH, "Jane", "Doe", Set.of());
    }

    @Test
    void register_trims_names_and_is_active() {
        User u = User.register(EMAIL, HASH, "  Jane  ", "  Doe ", Set.of());
        assertThat(u.getFirstName()).isEqualTo("Jane");
        assertThat(u.getLastName()).isEqualTo("Doe");
        assertThat(u.isActive()).isTrue();
        assertThat(u.getRoles()).isEmpty();
    }

    @Test
    void register_requires_email_and_password() {
        assertThatThrownBy(() -> User.register(null, HASH, "A", "B", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.register(EMAIL, null, "A", "B", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rename_rejects_blank() {
        User u = newUser();
        assertThatThrownBy(() -> u.rename("", "Doe")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> u.rename("Jane", "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activation_toggles() {
        User u = newUser();
        u.deactivate();
        assertThat(u.isActive()).isFalse();
        u.activate();
        assertThat(u.isActive()).isTrue();
        u.changeActivation(false);
        assertThat(u.isActive()).isFalse();
    }

    @Test
    void assignRoles_replaces_the_set_defensively() {
        User u = newUser();
        var roles = new java.util.HashSet<>(Set.of(Role.create("ADMIN", null, Set.of())));
        u.assignRoles(roles);
        roles.clear();
        assertThat(u.getRoles()).hasSize(1);
    }

    @Test
    void a_successful_login_resets_the_failure_counter() {
        User u = newUser();
        u.recordSuccessfulLogin();
        assertThat(u.getFailedLoginAttempts()).isZero();
    }

    @Test
    void reportRefreshTokenReuse_records_the_event() {
        User u = newUser();
        u.reportRefreshTokenReuse();
        assertThat(u.recordedEvents()).singleElement().isInstanceOf(RefreshTokenReused.class);
    }

    @Test
    void refresh_token_rotation_and_revocation_stay_inside_the_user_aggregate() {
        User u = newUser();
        RefreshToken first = u.startRefreshFamily("hash-1", Instant.now().plusSeconds(60));

        RefreshToken next = u.rotateRefreshToken(first, "hash-2", Instant.now().plusSeconds(60));
        u.revokeRefreshFamily(first.getFamilyId());

        assertThat(first.isRevoked()).isTrue();
        assertThat(next.isRevoked()).isTrue();
        assertThat(u.refreshToken("hash-2")).contains(next);
    }

    @Test
    void a_user_cannot_rotate_another_users_refresh_token() {
        User owner = newUser();
        User other = User.register(new Email("other@example.com"), HASH, "Other", "User", Set.of());
        RefreshToken token = owner.startRefreshFamily("hash-1", Instant.now().plusSeconds(60));

        assertThatThrownBy(() ->
                        other.rotateRefreshToken(token, "hash-2", Instant.now().plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivation_revokes_all_sessions() {
        User deactivated = newUser();
        RefreshToken deactivatedSession =
                deactivated.startRefreshFamily("deactivate", Instant.now().plusSeconds(60));
        deactivated.deactivate();

        assertThat(deactivatedSession.isRevoked()).isTrue();
    }
}
