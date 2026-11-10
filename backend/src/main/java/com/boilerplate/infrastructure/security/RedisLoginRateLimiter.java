package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.LoginRateLimiter;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Redis-backed atomic token bucket shared by all monolith instances. */
@Component
@Profile("distributed-rate-limit")
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final String KEY_PREFIX = "auth:login-rate:";
    private static final DefaultRedisScript<Long> TRY_ACQUIRE = new DefaultRedisScript<>(
            """
            local time = redis.call('TIME')
            local now = time[1] * 1000 + math.floor(time[2] / 1000)
            local capacity = tonumber(ARGV[1])
            local refillPerMinute = tonumber(ARGV[2])
            local state = redis.call('HMGET', KEYS[1], 'tokens', 'updated')
            local tokens = tonumber(state[1]) or capacity
            local updated = tonumber(state[2]) or now
            tokens = math.min(capacity, tokens + math.max(0, now - updated) * refillPerMinute / 60000)
            local allowed = 0
            if tokens >= 1 then
              tokens = tokens - 1
              allowed = 1
            end
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'updated', now)
            redis.call('PEXPIRE', KEYS[1], math.ceil(capacity * 60000 / refillPerMinute) + 60000)
            return allowed
            """,
            Long.class);

    private final StringRedisTemplate redis;
    private final RedisConnectionFactory connectionFactory;

    public RedisLoginRateLimiter(StringRedisTemplate redis, RedisConnectionFactory connectionFactory) {
        this.redis = redis;
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    void verifyRedisAvailability() {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.ping();
        }
    }

    @Override
    public boolean tryAcquire(String key, int capacity, int refillPerMinute) {
        Long result = redis.execute(
                TRY_ACQUIRE,
                List.of(KEY_PREFIX + RateLimitKeyDigest.digest(key)),
                Integer.toString(capacity),
                Integer.toString(refillPerMinute));
        return Long.valueOf(1L).equals(result);
    }
}
