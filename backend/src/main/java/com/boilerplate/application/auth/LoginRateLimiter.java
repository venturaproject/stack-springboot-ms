package com.boilerplate.application.auth;

/** Outbound port for throttling credential endpoints across the configured trust boundary. */
public interface LoginRateLimiter {

    boolean tryAcquire(String key, int capacity, int refillPerMinute);
}
