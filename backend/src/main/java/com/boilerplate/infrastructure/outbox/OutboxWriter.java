package com.boilerplate.infrastructure.outbox;

import com.boilerplate.domain.shared.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns the domain events that Spring Data JPA drains from an aggregate on {@code save()}
 * into {@code outbox_events} rows — written {@code BEFORE_COMMIT}, so they share the
 * aggregate's transaction. Nothing is published here; that's the relay's job, after commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OutboxWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void stage(DomainEvent event) {
        try {
            repository.save(OutboxEvent.capture(
                    event.eventType(),
                    aggregateTypeOf(event.eventType()),
                    event.aggregateId(),
                    objectMapper.writeValueAsString(event)));
            log.debug("staged {} to outbox", event.eventType());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise domain event " + event.eventType(), e);
        }
    }

    /** {@code orders.placed.v1} -> {@code orders}. */
    private static String aggregateTypeOf(String eventType) {
        int firstDot = eventType.indexOf('.');
        return firstDot > 0 ? eventType.substring(0, firstDot) : "unknown";
    }
}
