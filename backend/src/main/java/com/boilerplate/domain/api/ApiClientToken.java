package com.boilerplate.domain.api;

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

/** An issued access/refresh token pair for an {@link ApiClient}. */
@Entity
@Table(name = "api_client_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class ApiClientToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "access_token_hash", unique = true, nullable = false)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", unique = true, nullable = false)
    private String refreshTokenHash;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false)
    private List<String> scopes;

    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;

    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private ApiClientToken(
            String clientId,
            String accessTokenHash,
            String refreshTokenHash,
            List<String> scopes,
            Instant now,
            long accessTtlSecs,
            long refreshTtlSecs) {
        this.clientId = clientId;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.scopes = List.copyOf(scopes);
        this.createdAt = now;
        this.accessExpiresAt = now.plusSeconds(accessTtlSecs);
        this.refreshExpiresAt = now.plusSeconds(refreshTtlSecs);
    }

    public static ApiClientToken issue(
            String clientId,
            String accessTokenHash,
            String refreshTokenHash,
            List<String> scopes,
            Instant now,
            long accessTtlSecs,
            long refreshTtlSecs) {
        return new ApiClientToken(
                clientId, accessTokenHash, refreshTokenHash, scopes, now, accessTtlSecs, refreshTtlSecs);
    }

    public boolean isAccessExpired() {
        return accessExpiresAt.isBefore(Instant.now());
    }

    public boolean isRefreshExpired() {
        return refreshExpiresAt.isBefore(Instant.now());
    }
}
