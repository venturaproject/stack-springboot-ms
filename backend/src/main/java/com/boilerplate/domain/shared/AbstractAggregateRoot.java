package com.boilerplate.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

/**
 * Base for aggregate roots that emit domain events. An aggregate calls {@link #recordEvent}
 * from a semantic method; Spring Data JPA drains the events (via {@link DomainEvents}) when
 * the aggregate is {@code save()}d or {@code delete()}d, and the outbox listener persists
 * them in the same transaction.
 *
 * <p>The Spring Data {@code @DomainEvents} / {@code @AfterDomainEventPublication} annotations
 * are the one Spring dependency the domain layer allows — they are the persistence-port
 * vocabulary, same category as the {@code Repository} marker on the repository interfaces.
 */
@MappedSuperclass
public abstract class AbstractAggregateRoot implements RecordsEvents {

    @Transient
    private final transient List<DomainEvent> events = new ArrayList<>();

    /**
     * Optimistic-lock counter. Hibernate bumps it on every flush and a stale write throws
     * {@code OptimisticLockingFailureException} — this is what stops a background job (the
     * fulfillment saga confirming an order) and a request thread (a user cancelling it) from
     * silently clobbering each other. Surface it as an ETag and check {@code If-Match} at the
     * controller if you also need to catch a stale write across two HTTP round-trips.
     */
    @Version
    @Column(nullable = false)
    private long version;

    public long getVersion() {
        return version;
    }

    protected final void recordEvent(DomainEvent event) {
        events.add(event);
    }

    @DomainEvents
    @Override
    public List<DomainEvent> recordedEvents() {
        return List.copyOf(events);
    }

    @AfterDomainEventPublication
    @Override
    public void clearEvents() {
        events.clear();
    }
}
