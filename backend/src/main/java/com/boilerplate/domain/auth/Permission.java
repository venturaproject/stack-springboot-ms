package com.boilerplate.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** Reference data: a named, grantable capability ({@code resource:action}). Immutable once created. */
@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("NullAway.Init") // non-null fields are populated by Hibernate / factory methods
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    public static Permission create(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Permission name must not be blank");
        }
        Permission permission = new Permission();
        permission.name = name.trim();
        permission.description = description;
        return permission;
    }
}
