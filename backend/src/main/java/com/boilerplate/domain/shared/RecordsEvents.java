package com.boilerplate.domain.shared;

import java.util.List;

/** An aggregate root that accumulates {@link DomainEvent}s during a unit of work. */
public interface RecordsEvents {

    List<DomainEvent> recordedEvents();

    void clearEvents();
}
