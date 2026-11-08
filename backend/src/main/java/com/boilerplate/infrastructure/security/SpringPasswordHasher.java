package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.PasswordHasher;
import com.boilerplate.domain.auth.HashedPassword;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public HashedPassword hash(String rawPassword) {
        return new HashedPassword(
                Objects.requireNonNull(passwordEncoder.encode(rawPassword), "PasswordEncoder returned a null hash"));
    }

    @Override
    public boolean matches(String rawPassword, HashedPassword hashed) {
        return passwordEncoder.matches(rawPassword, hashed.value());
    }
}
