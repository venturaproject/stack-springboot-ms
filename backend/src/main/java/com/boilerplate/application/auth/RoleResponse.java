package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleResponse(UUID id, String name, String description, List<String> permissions, Instant createdAt) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(p -> p.getName()).toList(),
                role.getCreatedAt());
    }
}
