package com.boilerplate.infrastructure.messaging.upcast;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Migrates one on-the-wire event version to the next. Register one {@code @Component} per
 * hop ({@code v1 -> v2}, {@code v2 -> v3}, …); {@link EventUpcasterChain} walks them so a
 * consumer only ever deals with the latest shape, however old the message on the topic is.
 *
 * <p>This is how event schemas evolve without a schema registry: the {@code .vN} suffix in
 * {@code eventType()} is the version, producers only ever emit the current one, and old
 * messages already in flight are lifted forward here on the way in.
 */
public interface EventUpcaster {

    /** The event type this upcaster reads, e.g. {@code "auth.refresh_token_reused.v1"}. */
    String fromType();

    /** The event type it produces, e.g. {@code "auth.refresh_token_reused.v2"}. */
    String toType();

    /** Returns the payload rewritten to the {@link #toType()} shape. Must not mutate the input. */
    JsonNode upcast(JsonNode payload);
}
