package com.boilerplate.domain.auth.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** An account was temporarily locked after repeated failed logins. */
public record UserLocked(UUID userId, String email, Instant lockedUntil) implements DomainEvent {

    @Override
    public String eventType() {
        return "auth.user_locked.v1";
    }

    @Override
    public UUID aggregateId() {
        return userId;
    }
}
