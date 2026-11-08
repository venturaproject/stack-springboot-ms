package com.boilerplate.application.api;

import com.boilerplate.domain.api.ApiClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiClientResponse(
        UUID id,
        String name,
        @JsonProperty("client_id") String clientId,
        List<String> scopes,
        boolean active,
        @JsonProperty("last_used_at") Instant lastUsedAt,
        @JsonProperty("created_at") Instant createdAt) {
    public static ApiClientResponse from(ApiClient c) {
        return new ApiClientResponse(
                c.getId(),
                c.getName(),
                c.getClientId(),
                c.getScopes(),
                c.isActive(),
                c.getLastUsedAt(),
                c.getCreatedAt());
    }
}
