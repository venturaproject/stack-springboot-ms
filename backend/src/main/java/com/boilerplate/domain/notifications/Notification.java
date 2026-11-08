package com.boilerplate.domain.notifications;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * A user-facing notification. A plain entity, not an aggregate root — it holds no
 * invariants worth a domain event; the only state change is {@link #markRead()}. A
 * {@code null} recipient means "everyone" (a broadcast).
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / the factory
public class Notification implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean stored;

    @Nullable
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    private Notification(UUID id, @Nullable UUID userId, String title, String message, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
    }

    public static Notification create(@Nullable UUID userId, String title, String message, @Nullable String type) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Notification title must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message must not be blank");
        }
        String kind = type == null || type.isBlank() ? "info" : type.trim();
        return new Notification(UUID.randomUUID(), userId, title.trim(), message.trim(), kind);
    }

    public void markRead() {
        this.read = true;
    }

    @Nullable
    public UUID getUserId() {
        return userId;
    }

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
