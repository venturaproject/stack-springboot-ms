package com.boilerplate.domain.auth;

import com.boilerplate.domain.auth.event.RefreshTokenReused;
import com.boilerplate.domain.auth.event.UserDeactivated;
import com.boilerplate.domain.auth.event.UserLocked;
import com.boilerplate.domain.shared.AbstractAggregateRoot;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * User aggregate root. State changes go through intention-revealing methods — the
 * aggregate protects its own invariants. No setters, no Spring types (the Spring Security
 * {@code UserDetails} view is the infrastructure adapter {@code SecurityUser}).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class User extends AbstractAggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private Email email;

    @Column(name = "password_hash", nullable = false)
    private HashedPassword passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String avatarUrl;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "first_failed_login_at")
    @Nullable
    private Instant firstFailedLoginAt;

    @Column(name = "locked_until")
    @Nullable
    private Instant lockedUntil;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /** Register a new user. */
    public static User register(
            Email email, HashedPassword passwordHash, String firstName, String lastName, Set<Role> roles) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.active = true;
        user.rename(firstName, lastName);
        user.assignRoles(roles);
        return user;
    }

    public void rename(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public void changePassword(HashedPassword newHash) {
        if (newHash == null) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        this.passwordHash = newHash;
    }

    public void assignRoles(Set<Role> newRoles) {
        this.roles = new HashSet<>(newRoles == null ? Set.of() : newRoles);
    }

    public void changeAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        if (!active) {
            return;
        }
        this.active = false;
        recordEvent(new UserDeactivated(id, email.value()));
    }

    /** Convenience for admin edit screens that carry the flag directly. */
    public void changeActivation(boolean shouldBeActive) {
        if (shouldBeActive) {
            activate();
        } else {
            deactivate();
        }
    }

    // ── Login hardening ──────────────────────────────────────────────────────

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Count a failed login. Once {@code maxAttempts} accumulate within {@code window},
     * the account locks for {@code lockFor} and a {@link UserLocked} event is recorded.
     */
    public void recordFailedLogin(int maxAttempts, Duration window, Duration lockFor) {
        Instant now = Instant.now();
        if (firstFailedLoginAt == null || firstFailedLoginAt.plus(window).isBefore(now)) {
            firstFailedLoginAt = now;
            failedLoginAttempts = 0;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockFor);
            failedLoginAttempts = 0;
            firstFailedLoginAt = null;
            recordEvent(new UserLocked(id, email.value(), lockedUntil));
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        firstFailedLoginAt = null;
    }

    /** A refresh token from this user's family was replayed after rotation. */
    public void reportRefreshTokenReuse() {
        recordEvent(new RefreshTokenReused(id, email.value()));
    }
}
