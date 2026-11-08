package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boilerplate.domain.auth.event.RefreshTokenReused;
import com.boilerplate.domain.auth.event.UserLocked;
import java.time.Duration;
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
    void locks_after_the_configured_number_of_failed_logins_and_emits_UserLocked() {
        User u = newUser();
        Duration window = Duration.ofMinutes(15);
        Duration lock = Duration.ofMinutes(15);

        for (int i = 0; i < 4; i++) {
            u.recordFailedLogin(5, window, lock);
        }
        assertThat(u.isLocked()).isFalse();

        u.recordFailedLogin(5, window, lock); // 5th

        assertThat(u.isLocked()).isTrue();
        assertThat(u.getFailedLoginAttempts()).isZero(); // counter reset on lock
        assertThat(u.recordedEvents())
                .filteredOn(e -> e instanceof UserLocked)
                .singleElement()
                .isInstanceOfSatisfying(
                        UserLocked.class, e -> assertThat(e.userId()).isEqualTo(u.getId()));
    }

    @Test
    void a_successful_login_resets_the_failure_counter() {
        User u = newUser();
        u.recordFailedLogin(5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        u.recordFailedLogin(5, Duration.ofMinutes(15), Duration.ofMinutes(15));
        u.recordSuccessfulLogin();
        assertThat(u.getFailedLoginAttempts()).isZero();
    }

    @Test
    void reportRefreshTokenReuse_records_the_event() {
        User u = newUser();
        u.reportRefreshTokenReuse();
        assertThat(u.recordedEvents()).singleElement().isInstanceOf(RefreshTokenReused.class);
    }
}
