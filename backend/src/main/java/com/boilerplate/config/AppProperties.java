package com.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt, Cookie cookie, Cors cors, Media media, Outbox outbox, Inbox inbox, Saga saga, Security security) {
    public record Jwt(String secret, long accessTtlSecs, long refreshTtlSecs) {}

    public record Cookie(boolean secure, String sameSite) {}

    public record Cors(String allowedOrigins) {}

    public record Media(String dir) {}

    /**
     * Outbox relay tuning. {@code backoffBaseMs}/{@code backoffMaxMs} bound the exponential
     * retry delay ({@code base * 2^(attempts-1)}, capped); {@code retentionDays} is how long
     * published rows are kept before the maintenance sweep drops them.
     */
    public record Outbox(long pollMs, int maxAttempts, long backoffBaseMs, long backoffMaxMs, int retentionDays) {}

    /** {@code retentionDays}: how long a processed-event dedupe row is kept before the sweep drops it. */
    public record Inbox(int retentionDays) {}

    /**
     * Saga engine tuning. {@code recoveryAfterMinutes} is how long a saga instance may sit
     * in a non-terminal state before the recovery job re-drives it (covers a crash between
     * steps); {@code retentionDays} is how long completed instances are kept.
     */
    public record Saga(int recoveryAfterMinutes, int retentionDays) {}

    public record Security(RateLimit rateLimit) {

        /** Token-bucket per client IP on the login endpoints. */
        public record RateLimit(int capacity, int refillPerMinute) {
            public RateLimit {
                if (capacity < 1 || refillPerMinute < 1) {
                    throw new IllegalArgumentException("Login rate-limit capacity and refill must be positive");
                }
            }
        }
    }
}
