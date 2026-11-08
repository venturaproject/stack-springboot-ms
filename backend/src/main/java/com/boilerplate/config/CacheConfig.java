package com.boilerplate.config;

import com.boilerplate.application.dashboard.DashboardQueryService;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Spring Cache, backed by Caffeine (in-process). {@code recordStats()} feeds
 * {@code /actuator/prometheus} (hit ratio, evictions) automatically.
 *
 * <p>Named caches are declared up front so they exist — and are instrumented — from startup.
 * The pattern to copy: a read model / query method carries {@code @Cacheable}, and an
 * {@code AFTER_COMMIT} domain-event listener clears the entry when the underlying data
 * changes (see {@code infrastructure.cache.DashboardCacheEvictor}).
 *
 * <p><b>Multi-instance:</b> a Caffeine cache is per-JVM, so each instance can briefly serve a
 * slightly different value until its own copy is evicted. For a shared cache add
 * {@code spring-boot-starter-data-redis} and return a {@code RedisCacheManager} here under a
 * {@code redis} profile — nothing else changes.
 */
@Configuration
// One notch outside the transaction advisor: a cache hit must not open a DB transaction.
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig {

    private final Duration ttl;
    private final long maxSize;

    public CacheConfig(@Value("${app.cache.ttl:5m}") Duration ttl, @Value("${app.cache.max-size:1000}") long maxSize) {
        this.ttl = ttl;
        this.maxSize = maxSize;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(DashboardQueryService.CACHE));
        manager.setCaffeine(
                Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(ttl).recordStats());
        return manager;
    }
}
