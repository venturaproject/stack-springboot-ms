package com.boilerplate.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.boilerplate.application.shared.EventPublisher;
import com.boilerplate.config.AppProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxEventRepository repository;

    @Mock
    EventPublisher publisher;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        var props = new AppProperties(
                null,
                null,
                null,
                null,
                new AppProperties.Outbox(2000, 3, 1000, 300_000, 30),
                new AppProperties.Inbox(30),
                new AppProperties.Saga(5, 30),
                null);
        relay = new OutboxRelay(repository, publisher, props, new SimpleMeterRegistry());
    }

    private static OutboxEvent row() {
        return OutboxEvent.capture("customers.registered.v1", "customers", UUID.randomUUID(), "{}");
    }

    @Test
    void does_nothing_when_the_outbox_is_empty() {
        when(repository.claimDue(anyInt())).thenReturn(List.of());
        relay.drain();
        verifyNoInteractions(publisher);
    }

    @Test
    void publishes_and_marks_each_row() {
        OutboxEvent row = row();
        when(repository.claimDue(anyInt())).thenReturn(List.of(row));

        relay.drain();

        verify(publisher).publish(any());
        assertThat(row.getPublishedAt()).isNotNull();
        assertThat(row.getFailedAt()).isNull();
        verify(repository).save(row);
    }

    @Test
    void a_failed_publish_parks_the_row_for_a_backed_off_retry() {
        OutboxEvent row = row();
        when(repository.claimDue(anyInt())).thenReturn(List.of(row));
        doThrow(new RuntimeException("broker down")).when(publisher).publish(any());

        relay.drain();

        assertThat(row.getPublishedAt()).isNull();
        assertThat(row.getFailedAt()).isNull();
        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getNextAttemptAt()).isNotNull();
    }

    @Test
    void a_row_is_dead_lettered_after_max_attempts() {
        OutboxEvent row = row();
        when(repository.claimDue(anyInt())).thenReturn(List.of(row));
        doThrow(new RuntimeException("poison")).when(publisher).publish(any());

        relay.drain(); // 1
        relay.drain(); // 2
        relay.drain(); // 3 -> maxAttempts

        assertThat(row.getAttempts()).isEqualTo(3);
        assertThat(row.getFailedAt()).isNotNull();
    }
}
