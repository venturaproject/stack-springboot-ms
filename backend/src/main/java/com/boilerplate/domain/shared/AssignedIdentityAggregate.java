package com.boilerplate.domain.shared;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Base for aggregate roots that assign their own {@link UUID} identity in the factory
 * (rather than {@code @GeneratedValue}) so a creation event can carry the id before the
 * row is flushed. Without {@link Persistable}, Spring Data sees the non-null id and issues
 * a {@code merge} (a SELECT before every INSERT); this flag makes {@code save()} a plain
 * {@code persist} for a freshly-built aggregate, which also keeps {@code @CreationTimestamp}
 * populated on the returned instance.
 */
@MappedSuperclass
public abstract class AssignedIdentityAggregate extends AbstractAggregateRoot implements Persistable<UUID> {

    @Transient
    private boolean stored;

    @Override
    public boolean isNew() {
        return !stored;
    }

    @PostPersist
    @PostLoad
    void markStored() {
        this.stored = true;
    }
}
