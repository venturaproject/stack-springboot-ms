package com.boilerplate.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.boilerplate.domain.customers.event.CustomerRegistered;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock
    OutboxEventRepository repository;

    @Test
    void stages_a_row_with_the_event_type_aggregate_and_json_payload() {
        OutboxWriter writer = new OutboxWriter(repository, new ObjectMapper());
        UUID customerId = UUID.randomUUID();

        writer.stage(new CustomerRegistered(customerId, "a@b.com", "Alice"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent row = captor.getValue();

        assertThat(row.getEventType()).isEqualTo("customers.registered.v1");
        assertThat(row.getAggregateType()).isEqualTo("customers");
        assertThat(row.getAggregateId()).isEqualTo(customerId);
        assertThat(row.getPayload()).contains(customerId.toString()).contains("a@b.com");
        assertThat(row.getPublishedAt()).isNull();
    }
}
