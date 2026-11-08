package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.Permission;
import java.time.Instant;
import java.util.UUID;

public record PermissionResponse(UUID id, String name, String description, Instant createdAt) {
    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getDescription(), p.getCreatedAt());
    }
}
