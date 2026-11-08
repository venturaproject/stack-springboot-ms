package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        boolean active,
        List<String> roles,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getName()).toList(),
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getName())
                        .distinct()
                        .sorted()
                        .toList(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
