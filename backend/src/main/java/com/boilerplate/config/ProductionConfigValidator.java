package com.boilerplate.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Fails fast under the {@code prod} profile when a config value that is fine for local
 * development would be dangerous in production. A broken deploy that never starts beats one
 * that starts with a signing key of {@code "change-me-..."}.
 */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProductionConfigValidator {

    private static final String DEV_JWT_SECRET = "change-me-in-production-minimum-32-characters";

    private final AppProperties app;

    @PostConstruct
    void validate() {
        List<String> problems = new ArrayList<>();

        String secret = app.jwt().secret();
        if (secret == null || secret.isBlank() || DEV_JWT_SECRET.equals(secret)) {
            problems.add("app.jwt.secret (JWT_SECRET) is still the development default");
        } else if (secret.length() < 32) {
            problems.add("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
        }

        if (!app.cookie().secure()) {
            problems.add("app.cookie.secure (COOKIE_SECURE) must be true in production");
        }

        String origins = app.cors().allowedOrigins();
        if (origins == null || origins.isBlank() || origins.contains("localhost") || origins.contains("127.0.0.1")) {
            problems.add("app.cors.allowed-origins (CORS_ALLOWED_ORIGINS) still points at localhost");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start with production-unsafe configuration:\n  - " + String.join("\n  - ", problems));
        }
        log.info("Production configuration validated (jwt secret, secure cookie, CORS origins)");
    }
}
