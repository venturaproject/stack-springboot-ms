package com.boilerplate.infrastructure.messaging.inbox;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer-side counterpart to the outbox. The outbox only guarantees at-least-once
 * delivery, so any consumer whose reaction is more than a log line must dedupe. {@link
 * #claim} atomically records "this consumer has handled this event id" and reports whether
 * *this* call is the one that won — {@code false} means an earlier delivery (retry,
 * redelivery, a duplicate partition assignment during rebalance, …) already handled it, and
 * the caller must skip its side effect.
 *
 * <p>Call this inside the same transaction as the side effect it guards ({@code
 * REQUIRED}, the default): if the business write rolls back, the claim rolls back with it,
 * so a delivery that genuinely failed is still retried on the next redelivery instead of
 * being silently swallowed.
 */
@Component
@RequiredArgsConstructor
public class InboxGuard {

    private final ProcessedEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean claim(String consumer, UUID eventId) {
        return repository.insertIfAbsent(UUID.randomUUID(), consumer, eventId) > 0;
    }
}
