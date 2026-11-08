package com.boilerplate.domain.shared;

import java.util.UUID;

/**
 * Marker for something that happened in the domain and is worth telling the outside world
 * about. Events are immutable {@code record}s carrying only domain data — no ids that the
 * aggregate does not already hold, no infrastructure types.
 */
public interface DomainEvent {

    /**
     * Stable, versioned identifier — e.g. {@code "orders.placed.v1"}. This, not the
     * Java class name, is what the outbox stores and what a Kafka topic / consumer keys
     * off; the class can be renamed freely. The first dotted segment is the aggregate type
     * ({@code orders}, {@code products}, {@code customers}, {@code auth}, {@code api}).
     */
    String eventType();

    /**
     * Id of the aggregate instance this happened to. It becomes the Kafka message key, so
     * every event of one aggregate lands on the same partition and is delivered in order.
     * The event id (unique per event, for deduplication) travels separately — as the
     * outbox row id and the {@code event-id} header.
     */
    UUID aggregateId();
}
