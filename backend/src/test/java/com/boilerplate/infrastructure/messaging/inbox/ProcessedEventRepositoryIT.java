package com.boilerplate.infrastructure.messaging.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.support.AbstractRepositoryIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProcessedEventRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    ProcessedEventRepository repository;

    @Test
    void first_claim_of_an_event_id_inserts_a_redelivery_of_the_same_event_id_does_not() {
        UUID eventId = UUID.randomUUID();

        int first = repository.insertIfAbsent(UUID.randomUUID(), "security-event-consumer", eventId);
        int redelivery = repository.insertIfAbsent(UUID.randomUUID(), "security-event-consumer", eventId);

        assertThat(first).isEqualTo(1);
        assertThat(redelivery).isEqualTo(0);
    }

    @Test
    void the_same_event_id_can_be_claimed_independently_by_a_different_consumer() {
        UUID eventId = UUID.randomUUID();

        int consumerA = repository.insertIfAbsent(UUID.randomUUID(), "consumer-a", eventId);
        int consumerB = repository.insertIfAbsent(UUID.randomUUID(), "consumer-b", eventId);

        assertThat(consumerA).isEqualTo(1);
        assertThat(consumerB).isEqualTo(1);
    }
}
