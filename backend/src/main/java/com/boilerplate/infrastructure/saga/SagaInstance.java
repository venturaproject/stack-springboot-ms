package com.boilerplate.infrastructure.saga;

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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Persisted state of one saga run. The engine primitive: an orchestrator owns the state
 * names and transitions, this just stores where a given instance got to. Rows are created
 * with a native {@code INSERT ... ON CONFLICT DO NOTHING} (idempotent start) and then
 * loaded and {@code save()}d as the orchestrator advances them.
 */
@Entity
@Table(name = "saga_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // populated by Hibernate
public class SagaInstance {

    @Id
    private UUID id;

    @Column(name = "saga_type", nullable = false, updatable = false)
    private String sagaType;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String payload;

    @Nullable
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Nullable
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Non-terminal step: move to {@code state}, replace the payload, clear any stale error. */
    public void transitionTo(String state, String payload) {
        this.state = state;
        this.payload = payload;
        this.lastError = null;
    }

    /** Terminal step: {@code state} is COMPLETED or COMPENSATED; {@code error} explains a compensation. */
    public void finish(String state, @Nullable String error) {
        this.state = state;
        this.lastError = error;
        this.completedAt = Instant.now();
    }

    public boolean isRunning() {
        return completedAt == null;
    }
}
