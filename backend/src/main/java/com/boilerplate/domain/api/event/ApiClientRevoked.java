package com.boilerplate.domain.api.event;

import com.boilerplate.domain.shared.DomainEvent;
import java.util.UUID;

public record ApiClientRevoked(UUID id, String clientId) implements DomainEvent {

    @Override
    public String eventType() {
        return "api.client_revoked.v1";
    }

    @Override
    public UUID aggregateId() {
        return id;
    }
}
