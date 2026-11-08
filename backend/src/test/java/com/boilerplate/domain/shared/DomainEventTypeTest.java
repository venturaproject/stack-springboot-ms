package com.boilerplate.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.api.event.ApiClientRevoked;
import com.boilerplate.domain.auth.event.RefreshTokenReused;
import com.boilerplate.domain.auth.event.UserDeactivated;
import com.boilerplate.domain.auth.event.UserLocked;
import com.boilerplate.domain.customers.event.CustomerAddressChanged;
import com.boilerplate.domain.customers.event.CustomerRegistered;
import com.boilerplate.domain.orders.event.OrderCancelled;
import com.boilerplate.domain.orders.event.OrderConfirmed;
import com.boilerplate.domain.orders.event.OrderLineAdded;
import com.boilerplate.domain.orders.event.OrderPlaced;
import com.boilerplate.domain.products.event.ProductCreated;
import com.boilerplate.domain.products.event.ProductPriceChanged;
import com.boilerplate.domain.products.event.StockAdjusted;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The stable event-type strings are the wire contract — pin them. */
class DomainEventTypeTest {

    @Test
    void every_event_declares_a_stable_versioned_type_whose_first_segment_is_the_aggregate() {
        UUID id = UUID.randomUUID();
        List<DomainEvent> events = List.of(
                new CustomerRegistered(id, "a@b.com", "A"),
                new CustomerAddressChanged(id),
                new ProductCreated(id, "TSH-0001", "Tee"),
                new ProductPriceChanged(id, 1000, 1200, "USD"),
                new StockAdjusted(id, -1, 4, "order_placed"),
                new OrderPlaced(id, id, 1200, "USD"),
                new OrderConfirmed(id, id),
                new OrderCancelled(id),
                new OrderLineAdded(id, id, 2),
                new UserDeactivated(id, "a@b.com"),
                new UserLocked(id, "a@b.com", Instant.now()),
                new RefreshTokenReused(id, "a@b.com"),
                new ApiClientRevoked(id, "cli_x"));

        assertThat(events)
                .extracting(DomainEvent::eventType)
                .containsExactly(
                        "customers.registered.v1",
                        "customers.address_changed.v1",
                        "products.created.v1",
                        "products.price_changed.v1",
                        "products.stock_adjusted.v1",
                        "orders.placed.v1",
                        "orders.confirmed.v1",
                        "orders.cancelled.v1",
                        "orders.line_added.v1",
                        "auth.user_deactivated.v1",
                        "auth.user_locked.v1",
                        "auth.refresh_token_reused.v1",
                        "api.client_revoked.v1");
        assertThat(events).allSatisfy(e -> assertThat(e.eventType()).matches("[a-z_]+\\.[a-z_]+\\.v\\d+"));
    }

    @Test
    void every_event_exposes_its_aggregate_id_for_the_partition_key() {
        UUID id = UUID.randomUUID();
        List<DomainEvent> events = List.of(
                new CustomerRegistered(id, "a@b.com", "A"),
                new ProductCreated(id, "TSH-0001", "Tee"),
                new OrderPlaced(id, UUID.randomUUID(), 1200, "USD"),
                new OrderConfirmed(id, UUID.randomUUID()),
                new UserLocked(id, "a@b.com", Instant.now()),
                new ApiClientRevoked(id, "cli_x"));
        assertThat(events).allSatisfy(e -> assertThat(e.aggregateId()).isEqualTo(id));
    }
}
