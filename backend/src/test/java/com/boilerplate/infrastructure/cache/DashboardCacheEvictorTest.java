package com.boilerplate.infrastructure.cache;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.orders.event.OrderPlaced;
import com.boilerplate.domain.products.event.ProductPriceChanged;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class DashboardCacheEvictorTest {

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache cache;

    private DashboardCacheEvictor evictor;

    @BeforeEach
    void setUp() {
        evictor = new DashboardCacheEvictor(cacheManager);
    }

    @Test
    void clears_the_dashboard_cache_after_an_event_that_moves_one_of_its_numbers() {
        when(cacheManager.getCache("dashboard")).thenReturn(cache);

        evictor.onDomainEvent(new OrderPlaced(UUID.randomUUID(), UUID.randomUUID(), 2000, "USD"));

        verify(cache).clear();
    }

    @Test
    void ignores_an_event_that_does_not_change_a_dashboard_number() {
        // A price change is a real domain event but the dashboard only shows counts, not prices.
        evictor.onDomainEvent(new ProductPriceChanged(UUID.randomUUID(), 1000, 1500, "USD"));

        verifyNoInteractions(cacheManager);
    }
}
