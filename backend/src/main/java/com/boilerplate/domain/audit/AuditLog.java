package com.boilerplate.domain.audit;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * An append-only audit record: who did what to which entity, and (optionally) a JSON blob
 * of what changed. Written by {@code infrastructure.audit.DomainEventAuditListener} in the
 * same transaction as the change it describes. Immutable once created.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / the factory
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 100)
    private String entity;

    @Nullable
    @Column(name = "entity_id")
    private String entityId;

    @Nullable
    @Column(columnDefinition = "text")
    private String changes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    private AuditLog(
            String actorId, String action, String entity, @Nullable String entityId, @Nullable String changes) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.changes = changes;
    }

    public static AuditLog record(
            String actorId, String action, String entity, @Nullable String entityId, @Nullable String changes) {
        return new AuditLog(actorId, action, entity, entityId, changes);
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    @Nullable
    public String getChanges() {
        return changes;
    }
}
