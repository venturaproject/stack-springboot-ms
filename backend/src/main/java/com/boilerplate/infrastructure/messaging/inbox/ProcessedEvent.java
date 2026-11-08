package com.boilerplate.infrastructure.messaging.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A record that some consumer has already handled a given event id — the row {@link
 * ProcessedEventRepository#insertIfAbsent} either inserts (this delivery wins the race) or
 * silently skips (some earlier delivery already claimed it). Infrastructure, not domain: no
 * consumer ever reads these back, they exist purely to make {@code INSERT ... ON CONFLICT}
 * possible.
 */
@Entity
@Table(name = "inbox_processed_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // populated by Hibernate
public class ProcessedEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String consumer;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
