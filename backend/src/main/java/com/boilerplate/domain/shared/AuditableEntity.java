package com.boilerplate.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Opt-in auditing baseline for a new aggregate root: extend this (instead of, or alongside,
 * {@link AbstractAggregateRoot}) and add {@code created_at / updated_at / created_by /
 * updated_by} columns in the context's Flyway migration. {@link com.boilerplate.config.JpaAuditingConfig}
 * populates them — {@code *_by} from the authenticated principal, {@code "system"} for
 * background work.
 *
 * <p>The existing example contexts predate this and keep their own {@code created_at}
 * handling; new ones should use this for consistency. Uses only {@code org.springframework.data..}
 * types, which the domain layer is allowed to reference.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuppressWarnings("NullAway.Init") // fields are populated by the auditing listener on persist
public abstract class AuditableEntity extends AbstractAggregateRoot {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
