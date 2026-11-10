package com.boilerplate.infrastructure.security;

/**
 * Minimal in-memory token bucket used only by the local-development rate-limit adapter.
 */
public final class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerMinute / 60.0;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
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
    public synchronized boolean isFull() {
        return tokens >= capacity;
    }
}
