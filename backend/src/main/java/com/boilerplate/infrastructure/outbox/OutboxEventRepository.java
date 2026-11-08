package com.boilerplate.infrastructure.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface OutboxEventRepository extends Repository<OutboxEvent, UUID> {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);

    /** Dead-letter queue: rows the relay gave up on, newest first. */
    Page<OutboxEvent> findByFailedAtIsNotNullOrderByOccurredAtDesc(Pageable pageable);

    /**
     * Claims a batch of rows that are due for a publish attempt. {@code FOR UPDATE SKIP
     * LOCKED} lets several application instances run the relay without stepping on each
     * other; the {@code next_attempt_at} guard implements the exponential backoff.
     */
    @Query(
            value =
                    """
        SELECT * FROM outbox_events
        WHERE published_at IS NULL AND failed_at IS NULL
          AND (next_attempt_at IS NULL OR next_attempt_at <= now())
        ORDER BY occurred_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """,
            nativeQuery = true)
    List<OutboxEvent> claimDue(@Param("limit") int limit);

    long countByPublishedAtIsNullAndFailedAtIsNull();

    long countByFailedAtIsNotNull();

    /** Retention sweep: drop rows that were published before {@code cutoff}. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM OutboxEvent e WHERE e.publishedAt IS NOT NULL AND e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
