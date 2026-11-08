package com.boilerplate.application.auth;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.storage.ImageUpload;
import com.boilerplate.application.storage.MediaStoragePort;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.Role;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.domain.exception.DuplicateEntityException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final MediaStoragePort mediaStorage;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> list(int page, int size) {
        return PagedResponse.from(
                userRepository.findAll(PageRequest.of(Math.max(0, page - 1), size)), UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findById(id));
    }

    @Transactional
    public UserResponse create(CreateUserCommand cmd) {
        Email email = new Email(cmd.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEntityException("Email already in use");
        }

        User user = User.register(
                email,
                passwordHasher.hash(cmd.password()),
                cmd.firstName(),
                cmd.lastName(),
                resolveRoles(cmd.roleIds()));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserCommand cmd) {
        User user = findById(id);
        user.rename(cmd.firstName(), cmd.lastName());
        user.changeActivation(cmd.active());

        if (cmd.roleIds() != null) {
            user.assignRoles(resolveRoles(cmd.roleIds()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        userRepository.delete(findById(id));
    }

    @Transactional
    public UserResponse updateMe(UUID userId, UpdateProfileCommand cmd) {
        User user = findById(userId);
        user.rename(cmd.firstName(), cmd.lastName());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse uploadAvatar(UUID userId, ImageUpload image) {
        User user = findById(userId);
        user.changeAvatar(mediaStorage.storeAvatar(userId, image));
        return UserResponse.from(userRepository.save(user));
    }

    private Set<Role> resolveRoles(List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        return roleIds.stream()
                .map(roleId -> roleRepository
                        .findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId)))
                .collect(Collectors.toSet());
    }

    private User findById(UUID id) {
        return userRepository
                .findDetailedById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
