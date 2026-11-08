package com.boilerplate.infrastructure.outbox;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A serialised domain event awaiting (or past) publication. Infrastructure, not domain. */
@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // populated by Hibernate / the factory
public class OutboxEvent {

    /** Unique per event — the deduplication key consumers see as the {@code event-id} header. */
    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /** Id of the aggregate instance — the Kafka message key, so its events stay ordered. */
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Nullable
    @Column(name = "published_at")
    private Instant publishedAt;

    @Nullable
    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(nullable = false)
    private int attempts;

    /** When the relay may next try this row. {@code null} = eligible now. Set by the backoff. */
    @Nullable
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    private OutboxEvent(String eventType, String aggregateType, UUID aggregateId, String payload) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.occurredAt = Instant.now();
        this.attempts = 0;
    }

    public static OutboxEvent capture(String eventType, String aggregateType, UUID aggregateId, String payload) {
        return new OutboxEvent(eventType, aggregateType, aggregateId, payload);
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }

    /** Records a failed publish and backs off: the relay skips this row until {@code now + backoff}. */
    public void recordFailure(Duration backoff) {
        this.attempts++;
        this.nextAttemptAt = Instant.now().plus(backoff);
    }

    public void deadLetter() {
        this.failedAt = Instant.now();
    }

    /** Admin action: take a dead-lettered row back into the relay's queue for a fresh run. */
    public void replay() {
        this.failedAt = null;
        this.attempts = 0;
        this.nextAttemptAt = null;
    }
}
