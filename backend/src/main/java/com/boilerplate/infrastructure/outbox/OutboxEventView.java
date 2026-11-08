package com.boilerplate.infrastructure.outbox;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

/** Read model for the dead-letter admin endpoint. */
public record OutboxEventView(
        UUID id,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        int attempts,
        Instant occurredAt,
        @Nullable Instant failedAt,
        String payload) {
    static OutboxEventView from(OutboxEvent e) {
        return new OutboxEventView(
                e.getId(),
                e.getEventType(),
                e.getAggregateType(),
                e.getAggregateId(),
                e.getAttempts(),
                e.getOccurredAt(),
                e.getFailedAt(),
                e.getPayload());
    }
}
