package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.LoginRateLimiter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Development fallback only; production startup requires the distributed Redis adapter. */
@Component
@Profile("!distributed-rate-limit")
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private static final int MAX_BUCKETS = 10_000;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int capacity, int refillPerMinute) {
        return bucketFor(RateLimitKeyDigest.digest(key), capacity, refillPerMinute)
                .map(TokenBucket::tryConsume)
                .orElse(false);
    }

    private synchronized Optional<TokenBucket> bucketFor(String key, int capacity, int refillPerMinute) {
        TokenBucket existing = buckets.get(key);
        if (existing != null) {
            return Optional.of(existing);
        }
        if (buckets.size() >= MAX_BUCKETS) {
            buckets.values().removeIf(TokenBucket::isFull);
            if (buckets.size() >= MAX_BUCKETS) {
                return Optional.empty();
            }
        }
        TokenBucket bucket = new TokenBucket(capacity, refillPerMinute);
        buckets.put(key, bucket);
        return Optional.of(bucket);
    }

    @Scheduled(fixedDelay = 600_000L)
    void sweepIdleBuckets() {
        buckets.values().removeIf(TokenBucket::isFull);
    }
}
