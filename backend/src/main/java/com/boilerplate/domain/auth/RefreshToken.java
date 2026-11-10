package com.boilerplate.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A refresh token child entity belonging to the {@link User} aggregate and a rotation
 * <em>family</em>. A login starts a family; each
 * refresh revokes the presented token and issues its successor in the same family.
 * Presenting an already-revoked token means it leaked — {@code AuthService} then revokes
 * the whole family.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private RefreshToken(String tokenHash, User user, Instant expiresAt, UUID familyId) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.familyId = familyId;
        this.revoked = false;
    }

    /** First token of a new session. */
    public static RefreshToken startFamily(String tokenHash, User user, Instant expiresAt) {
        require(tokenHash, user);
        return new RefreshToken(tokenHash, user, expiresAt, UUID.randomUUID());
    }

    /** Successor token in this token's family. The caller revokes {@code this}. */
    public RefreshToken rotate(String newTokenHash, Instant expiresAt) {
        require(newTokenHash, user);
        return new RefreshToken(newTokenHash, user, expiresAt, familyId);
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    private static void require(String token, User user) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
    }
}
