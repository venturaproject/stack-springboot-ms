package com.boilerplate.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.customers.Customer;
import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.support.AbstractIntegrationIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

/** {@code @Cacheable} on the dashboard query + {@code DashboardCacheEvictor} clearing it on a domain event. */
class DashboardCachingIT extends AbstractIntegrationIT {

    @Autowired
    DashboardQueryService dashboard;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CacheManager cacheManager;

    private Object cachedDashboard() {
        Cache cache = cacheManager.getCache(DashboardQueryService.CACHE);
        Cache.ValueWrapper wrapper = cache == null ? null : cache.get(SimpleKey.EMPTY);
        return wrapper == null ? null : wrapper.get();
    }

    @Test
    void the_response_is_cached_and_a_new_customer_evicts_it() {
        dashboard.query();
        assertThat(cachedDashboard()).as("cached after first call").isNotNull();

        customers.save(
                Customer.register("Cache Tester", new Email("cache-" + UUID.randomUUID() + "@example.com"), null));

        assertThat(cachedDashboard())
                .as("evicted after customers.registered.v1")
                .isNull();
    }
}
