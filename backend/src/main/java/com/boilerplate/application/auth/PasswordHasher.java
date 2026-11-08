package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.HashedPassword;

/**
 * Outbound port for password hashing. The concrete algorithm (Argon2id via Spring
 * Security) lives in {@code infrastructure.security}.
 */
public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean matches(String rawPassword, HashedPassword hashed);
}
