package com.boilerplate.application.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain event on its way out of the system. Carries the serialised payload so a
 * publisher adapter can route it without depending on the concrete event classes:
 * {@code aggregateType} picks the topic, {@code aggregateId} is the partition key (keeps
 * one aggregate's events ordered), {@code id} is the unique per-event id for consumer
 * deduplication.
 */
public record PublishedEvent(
        UUID id, String type, String aggregateType, UUID aggregateId, String payloadJson, Instant occurredAt) {}
