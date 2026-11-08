package com.boilerplate.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.boilerplate.application.dispatch.CommandBus;
import com.boilerplate.application.products.CreateProductCommand;
import com.boilerplate.application.products.ProductResponse;
import com.boilerplate.application.shared.EventPublisher;
import com.boilerplate.application.shared.PublishedEvent;
import com.boilerplate.support.AbstractIntegrationIT;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(OutboxIntegrationIT.CapturingPublisherConfig.class)
class OutboxIntegrationIT extends AbstractIntegrationIT {

    @Autowired
    CommandBus commandBus;

    @Autowired
    CapturedEvents captured;

    @Test
    void creating_a_product_stages_an_event_that_the_relay_publishes() {
        captured.events.clear();
        String sku = "OBX-" + ThreadLocalRandom.current().nextInt(1000, 9999);

        ProductResponse created =
                commandBus.dispatch(new CreateProductCommand(sku, "Outbox demo", null, 1000, "USD", 10));

        assertThat(created.id()).isNotNull();

        // The relay drains on its own schedule, so assert the end-to-end outcome: the event
        // reaches the publisher, on the right topic, keyed by the aggregate id.
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(captured.events).anySatisfy(e -> {
                    assertThat(e.type()).isEqualTo("products.created.v1");
                    assertThat(e.aggregateType()).isEqualTo("products");
                    assertThat(e.aggregateId()).isEqualTo(created.id());
                }));
    }

    static final class CapturedEvents {
        final List<PublishedEvent> events = new CopyOnWriteArrayList<>();
    }

    @TestConfiguration
    static class CapturingPublisherConfig {

        @Bean
        CapturedEvents capturedEvents() {
            return new CapturedEvents();
        }

        @Bean
        @Primary
        EventPublisher capturingEventPublisher(CapturedEvents capturedEvents) {
            return capturedEvents.events::add;
        }
    }
}
