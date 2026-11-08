package com.boilerplate.domain.auth.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

/** A revoked refresh token was replayed — the family was treated as compromised. */
public record RefreshTokenReused(UUID userId, String email) implements DomainEvent {

    @Override
    public String eventType() {
        return "auth.refresh_token_reused.v1";
    }

    @Override
    public UUID aggregateId() {
        return userId;
    }
}
