package com.boilerplate.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.HashedPassword;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    com.boilerplate.application.storage.MediaStoragePort mediaStorage;

    @InjectMocks
    UserService service;

    private User existing() {
        return User.register(new Email("jane@x.com"), new HashedPassword("$stub"), "Jane", "Doe", Set.of());
    }

    @Test
    void create_rejects_a_duplicate_email() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(() ->
                        service.create(new CreateUserCommand("jane@x.com", "password123", "Jane", "Doe", List.of())))
                .isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void create_hashes_the_password_through_the_port() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(new HashedPassword("$argon2id$x"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = service.create(new CreateUserCommand("Jane@X.com", "password123", "Jane", "Doe", List.of()));

        assertThat(res.email()).isEqualTo("jane@x.com");
        verify(passwordHasher).hash("password123");
    }

    @Test
    void create_fails_when_a_referenced_role_does_not_exist() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.create(new CreateUserCommand("a@b.com", "password123", "A", "B", List.of(roleId))))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_applies_rename_and_activation() {
        User user = existing();
        when(userRepository.findDetailedById(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(UUID.randomUUID(), new UpdateUserCommand("Janet", "Roe", false, null));

        assertThat(user.getFirstName()).isEqualTo("Janet");
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void getById_on_a_missing_user_is_a_404() {
        when(userRepository.findDetailedById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(UUID.randomUUID())).isInstanceOf(EntityNotFoundException.class);
    }
}
