package com.boilerplate.domain.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Persistence port for the {@link User} aggregate. Extends the minimal Spring Data
 * {@code Repository} marker (not {@code JpaRepository}) so the domain contract exposes
 * only what the domain needs — see ARCHITECTURE.md.
 */
public interface UserRepository extends Repository<User, UUID> {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    /** User with roles and permissions eagerly fetched — for authentication. */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findDetailedByEmail(Email email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findDetailedById(UUID id);

    /** Loads the owning aggregate and its session children for token validation/rotation. */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "refreshTokens"})
    @Query("select u from User u join u.refreshTokens rt where rt.tokenHash = :tokenHash")
    Optional<User> findDetailedByRefreshTokenHash(@Param("tokenHash") String tokenHash);

    boolean existsByEmail(Email email);

    Page<User> findAll(Pageable pageable);

    long count();

    User save(User user);

    void delete(User user);
}
