package com.boilerplate.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RedisLoginRateLimiterIT {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);

    @Test
    void shared_bucket_is_atomic_under_concurrent_requests() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            RedisLoginRateLimiter limiter = new RedisLoginRateLimiter(template, connectionFactory);
            limiter.verifyRedisAvailability();
            String key = "test|" + UUID.randomUUID();

            try (var executor = Executors.newFixedThreadPool(12)) {
                long allowed = IntStream.range(0, 48)
                        .mapToObj(ignored -> executor.submit(() -> limiter.tryAcquire(key, 3, 1)))
                        .mapToLong(future -> {
                            try {
                                return future.get() ? 1 : 0;
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .sum();
                assertThat(allowed).isEqualTo(3);
            }
        } finally {
            connectionFactory.destroy();
        }
    }
}
