package com.boilerplate.domain.api;

import com.boilerplate.domain.api.event.ApiClientRevoked;
import com.boilerplate.domain.shared.AbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * External API client (client-credentials). Aggregate root for the {@code api} context.
 */
@Entity
@Table(name = "api_clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class ApiClient extends AbstractAggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false)
    private List<String> scopes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private ApiClient(String name, String clientId, String secretHash, List<String> scopes, Instant createdAt) {
        this.name = name;
        this.clientId = clientId;
        this.secretHash = secretHash;
        this.scopes = List.copyOf(scopes);
        this.active = true;
        this.createdAt = createdAt;
    }

    public static ApiClient issue(String name, String clientId, String secretHash, List<String> scopes, Instant now) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("API client name must not be blank");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("API client must have at least one scope");
        }
        return new ApiClient(name, clientId, secretHash, scopes, now);
    }

    public boolean secretMatches(String presentedSecretHash) {
        return secretHash.equals(presentedSecretHash);
    }

    public void revoke() {
        if (!active) {
            return;
        }
        this.active = false;
        recordEvent(new ApiClientRevoked(id, clientId));
    }

    public void recordUsage(Instant when) {
        this.lastUsedAt = when;
    }
}
