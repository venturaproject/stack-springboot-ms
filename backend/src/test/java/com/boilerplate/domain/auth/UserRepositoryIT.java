package com.boilerplate.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.support.AbstractRepositoryIT;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Test
    void findDetailedByEmail_fetches_roles_and_permissions() {
        Permission read = permissionRepository.save(Permission.create("things:read", null));
        Role role = roleRepository.save(Role.create("READER", null, Set.of(read)));
        Email email = new Email("detail@example.com");
        userRepository.save(User.register(email, new HashedPassword("$argon2id$x"), "De", "Tail", Set.of(role)));

        User found = userRepository.findDetailedByEmail(email).orElseThrow();

        assertThat(found.getRoles()).extracting(Role::getName).containsExactly("READER");
        assertThat(found.getRoles().iterator().next().getPermissions())
                .extracting(Permission::getName)
                .containsExactly("things:read");
    }

    @Test
    void existsByEmail_uses_the_value_object() {
        userRepository.save(
                User.register(new Email("Exists@Example.com"), new HashedPassword("$argon2id$x"), "E", "X", Set.of()));

        assertThat(userRepository.existsByEmail(new Email("exists@example.com")))
                .isTrue();
        assertThat(userRepository.existsByEmail(new Email("nope@example.com"))).isFalse();
    }

    @Test
    void the_lockout_columns_round_trip() {
        Email email = new Email("locked@example.com");
        User user = User.register(email, new HashedPassword("$argon2id$x"), "L", "U", Set.of());
        user.recordFailedLogin(1, java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15));
        userRepository.save(user);

        User reloaded = userRepository.findByEmail(email).orElseThrow();
        assertThat(reloaded.isLocked()).isTrue();
        assertThat(reloaded.getLockedUntil()).isAfter(Instant.now());
    }
}
