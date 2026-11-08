package com.boilerplate.application.audit;

import com.boilerplate.domain.audit.AuditLog;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String actorId,
        String action,
        String entity,
        @Nullable String entityId,
        @Nullable String changes,
        Instant createdAt) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActorId(),
                a.getAction(),
                a.getEntity(),
                a.getEntityId(),
                a.getChanges(),
                a.getCreatedAt());
    }
}
