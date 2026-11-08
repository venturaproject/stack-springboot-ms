package com.boilerplate.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.infrastructure.messaging.inbox.InboxGuard;
import com.boilerplate.infrastructure.messaging.upcast.EventUpcasterChain;
import com.boilerplate.infrastructure.messaging.upcast.RefreshTokenReusedV1ToV2Upcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityEventConsumerTest {

    @Mock
    InboxGuard inboxGuard;

    private SecurityEventConsumer consumer;

    @BeforeEach
    void setUp() {
        var chain = new EventUpcasterChain(List.of(new RefreshTokenReusedV1ToV2Upcaster()), new ObjectMapper());
        consumer = new SecurityEventConsumer(inboxGuard, chain);
    }

    @Test
    void a_redelivered_event_is_skipped_once_the_inbox_has_already_claimed_it() {
        UUID eventId = UUID.randomUUID();
        when(inboxGuard.claim("security-event-consumer", eventId)).thenReturn(false);

        consumer.onAuthEvent(refreshTokenReusedRecord(eventId));

        verify(inboxGuard).claim("security-event-consumer", eventId);
    }

    @Test
    void first_delivery_is_processed_after_a_successful_claim() {
        UUID eventId = UUID.randomUUID();
        when(inboxGuard.claim("security-event-consumer", eventId)).thenReturn(true);

        consumer.onAuthEvent(refreshTokenReusedRecord(eventId));

        verify(inboxGuard).claim("security-event-consumer", eventId);
    }

    @Test
    void an_unrelated_event_type_is_never_claimed() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "boilerplate.auth", 0, 0L, UUID.randomUUID().toString(), "{}");
        record.headers()
                .add(new RecordHeader("event-type", "auth.password_changed.v1".getBytes(StandardCharsets.UTF_8)));
        record.headers()
                .add(new RecordHeader("event-id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

        consumer.onAuthEvent(record);

        verify(inboxGuard, never()).claim(any(), any());
    }

    private static ConsumerRecord<String, String> refreshTokenReusedRecord(UUID eventId) {
        UUID userId = UUID.randomUUID();
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "boilerplate.auth", 0, 0L, userId.toString(), "{\"userId\":\"" + userId + "\",\"email\":\"a@b.com\"}");
        record.headers()
                .add(new RecordHeader("event-type", "auth.refresh_token_reused.v1".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("event-id", eventId.toString().getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
