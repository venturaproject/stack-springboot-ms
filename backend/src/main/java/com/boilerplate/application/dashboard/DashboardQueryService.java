package com.boilerplate.application.dashboard;

import com.boilerplate.domain.customers.CustomerRepository;
import com.boilerplate.domain.orders.OrderRepository;
import com.boilerplate.domain.orders.OrderStatus;
import com.boilerplate.domain.products.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example read model: cross-aggregate counts assembled for the dashboard. A query service,
 * not a repository concern — this shape is UI-driven.
 *
 * <p>Seven count queries across three tables — the reference case for {@code @Cacheable}:
 * read-heavy, tolerates a few seconds of staleness. {@code infrastructure.cache.DashboardCacheEvictor}
 * clears the entry {@code AFTER_COMMIT} of any order/product/customer domain event so it is
 * never stale for long.
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    /** Cache name for {@link #query()} — declared in {@code CacheConfig}, evicted by {@code DashboardCacheEvictor}. */
    public static final String CACHE = "dashboard";

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public DashboardResponse query() {
        return new DashboardResponse(
                productRepository.count(),
                productRepository.countByStockQuantityLessThanEqual(LOW_STOCK_THRESHOLD),
                customerRepository.count(),
                orderRepository.count(),
                orderRepository.countByStatus(OrderStatus.DRAFT),
                orderRepository.countByStatus(OrderStatus.PLACED),
                orderRepository.countByStatus(OrderStatus.CANCELLED));
    }
}
