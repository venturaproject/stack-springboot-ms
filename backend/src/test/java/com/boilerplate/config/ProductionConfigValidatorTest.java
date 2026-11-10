package com.boilerplate.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigValidatorTest {

    private final AppProperties safeApp = new AppProperties(
            new AppProperties.Jwt("a-production-secret-with-more-than-32-characters", 900, 604800),
            new AppProperties.Cookie(true, "Strict"),
            new AppProperties.Cors("https://app.example.com"),
            new AppProperties.Media("/srv/media"),
            new AppProperties.Outbox(2000, 10, 1000, 300_000, 30),
            new AppProperties.Inbox(30),
            new AppProperties.Saga(5, 30),
            new AppProperties.Security(new AppProperties.Security.RateLimit(5, 5)));

    @Test
    void refuses_production_without_the_distributed_rate_limiter_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new ProductionConfigValidator(safeApp, environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("distributed-rate-limit");
    }

    @Test
    void accepts_production_with_the_distributed_rate_limiter_profile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "distributed-rate-limit");
        environment.setProperty("spring.data.redis.host", "redis.internal.example");

        new ProductionConfigValidator(safeApp, environment).validate();
    }

    @Test
    void refuses_a_local_or_missing_redis_endpoint_in_production() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "distributed-rate-limit");
        environment.setProperty("spring.data.redis.host", "localhost");

        assertThatThrownBy(() -> new ProductionConfigValidator(safeApp, environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-local Redis service");
    }
}
