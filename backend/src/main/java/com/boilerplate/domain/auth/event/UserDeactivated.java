package com.boilerplate.domain.auth.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record UserDeactivated(UUID userId, String email) implements DomainEvent {

    @Override
    public String eventType() {
        return "auth.user_deactivated.v1";
    }

    @Override
    public UUID aggregateId() {
        return userId;
    }
}
