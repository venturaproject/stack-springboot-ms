package com.boilerplate.infrastructure.messaging.inbox;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface ProcessedEventRepository extends Repository<ProcessedEvent, UUID> {

    /**
     * Atomically claims {@code (consumer, eventId)}. Returns {@code 1} if this call
     * inserted the row (first delivery, or first to win the race under concurrent
     * consumers), {@code 0} if it already existed (a redelivery — the outbox is
     * at-least-once, so this is expected, not an error).
     */
    @Modifying
    @Query(
            value =
                    """
        INSERT INTO inbox_processed_events (id, consumer, event_id, processed_at)
        VALUES (:id, :consumer, :eventId, now())
        ON CONFLICT (consumer, event_id) DO NOTHING
        """,
            nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("consumer") String consumer, @Param("eventId") UUID eventId);

    /** Retention sweep: dedupe rows older than {@code cutoff} can't match a live redelivery. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoff")
    int deleteProcessedBefore(@Param("cutoff") Instant cutoff);
}
