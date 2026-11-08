package com.boilerplate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.application.auth.PasswordHasher;
import com.boilerplate.domain.auth.Permission;
import com.boilerplate.domain.auth.PermissionRepository;
import com.boilerplate.domain.auth.Role;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class DataInitializerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);

    @BeforeEach
    void configureRepositories() {
        when(permissions.findByName(anyString())).thenReturn(java.util.Optional.empty());
        when(permissions.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roles.findByName(anyString())).thenReturn(java.util.Optional.empty());
        when(roles.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void refuses_to_start_an_empty_database_without_bootstrap_credentials() {
        when(users.count()).thenReturn(0L);

        assertThatThrownBy(() -> initializer(new StandardEnvironment()).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD");

        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    void refuses_a_weak_first_admin_password_in_production() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("prod");
        when(users.count()).thenReturn(0L);

        DataInitializer initializer = initializer(environment);
        ReflectionTestUtils.setField(initializer, "bootstrapAdminEmail", "owner@example.com");
        ReflectionTestUtils.setField(initializer, "bootstrapAdminPassword", "admin123");

        assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 20 characters");
    }

    @Test
    void does_not_reset_or_recreate_an_admin_when_users_already_exist() {
        when(users.count()).thenReturn(1L);

        initializer(new StandardEnvironment()).run(new DefaultApplicationArguments());

        verify(users, never()).save(any(User.class));
        verify(passwordHasher, never()).hash(anyString());
    }

    private DataInitializer initializer(StandardEnvironment environment) {
        return new DataInitializer(users, roles, permissions, passwordHasher, environment);
    }
}
