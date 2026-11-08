package com.boilerplate.infrastructure.cache;

import com.boilerplate.application.dashboard.DashboardQueryService;
import com.boilerplate.domain.shared.DomainEvent;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event-driven cache invalidation — the counterpart of {@code @Cacheable} on
 * {@link DashboardQueryService#query()}. When a domain event that changes one of the
 * dashboard's numbers commits, the cached response is dropped so the next reader recomputes.
 *
 * <p>Runs {@code AFTER_COMMIT}: the data the dashboard would recount is already durable, so
 * there is no window where the cache is cleared but the change is not yet visible. This is
 * the pattern to copy for any read model with a {@code @Cacheable} query.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardCacheEvictor {

    /** Events whose commit changes a number the dashboard shows. */
    private static final Set<String> AFFECTS_DASHBOARD = Set.of(
            "orders.placed.v1",
            "orders.confirmed.v1",
            "orders.cancelled.v1",
            "products.created.v1",
            "products.stock_adjusted.v1",
            "customers.registered.v1");

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainEvent(DomainEvent event) {
        if (!AFFECTS_DASHBOARD.contains(event.eventType())) {
            return;
        }
        Cache cache = cacheManager.getCache(DashboardQueryService.CACHE);
        if (cache != null) {
            cache.clear();
            log.debug("dashboard cache cleared after {}", event.eventType());
        }
    }
}
