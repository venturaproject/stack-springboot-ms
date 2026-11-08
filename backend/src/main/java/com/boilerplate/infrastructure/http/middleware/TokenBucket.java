package com.boilerplate.infrastructure.http.middleware;

/**
 * Minimal in-memory token bucket. Deliberately dependency-free — swap for Bucket4j +
 * Redis if you need distributed limiting across instances.
 */
final class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillNanos;

    TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerMinute / 60.0;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryConsume() {
        long now = System.nanoTime();
        double elapsed = (now - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsed * refillPerSecond);
        lastRefillNanos = now;
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /** Idle (untouched) — safe for the sweeper to evict. */
    synchronized boolean isFull() {
        return tokens >= capacity;
    }
}
