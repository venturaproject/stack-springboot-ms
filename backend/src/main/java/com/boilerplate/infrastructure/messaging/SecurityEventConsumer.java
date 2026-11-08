package com.boilerplate.infrastructure.messaging;

import com.boilerplate.infrastructure.messaging.inbox.InboxGuard;
import com.boilerplate.infrastructure.messaging.upcast.EventUpcasterChain;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example downstream consumer — closes the loop that the outbox opens. Reacts to auth
 * events off the {@code <prefix>.auth} topic; here it just logs, but this is where you'd
 * send the "your account was accessed from a new token" email on reuse detection.
 *
 * <p>It shows the two things every real consumer needs:
 * <ul>
 *   <li><b>Dedup</b> via {@link InboxGuard} keyed on the {@code event-id} header — the
 *       outbox is at-least-once, so a redelivery must not act twice.</li>
 *   <li><b>Upcasting</b> via {@link EventUpcasterChain} — the payload is lifted to the
 *       latest schema version before it is read, so an old message on the topic is a
 *       non-event.</li>
 * </ul>
 */
@Component
@Profile("kafka")
@RequiredArgsConstructor
@Slf4j
class SecurityEventConsumer {

    private static final String CONSUMER_NAME = "security-event-consumer";
    private static final String HANDLED_TYPE = "auth.refresh_token_reused.v2";

    private final InboxGuard inboxGuard;
    private final EventUpcasterChain upcasterChain;

    @KafkaListener(
            topics = "${app.events.kafka.topic-prefix:boilerplate}.auth",
            groupId = "${app.events.kafka.security-group:boilerplate-security}")
    @Transactional
    void onAuthEvent(ConsumerRecord<String, String> record) {
        String rawType = header(record, "event-type");
        if (rawType == null || !rawType.startsWith("auth.refresh_token_reused.")) {
            return;
        }

        String eventId = header(record, "event-id");
        if (eventId == null) {
            log.warn("auth event with no event-id header, cannot dedupe — processing anyway");
        } else if (!inboxGuard.claim(CONSUMER_NAME, UUID.fromString(eventId))) {
            log.debug("skipping already-processed event {}", eventId);
            return;
        }

        EventUpcasterChain.Upcasted upcasted = upcasterChain.upcast(rawType, record.value());
        if (!HANDLED_TYPE.equals(upcasted.type())) {
            log.warn("expected {} after upcasting {}, got {}", HANDLED_TYPE, rawType, upcasted.type());
            return;
        }

        JsonNode payload = upcasted.payload();
        log.warn(
                "SECURITY: refresh-token reuse for user {} <{}> — would send a breach-notification email",
                payload.path("userId").asText(),
                payload.path("email").asText());
    }

    @Nullable
    private static String header(ConsumerRecord<?, ?> record, String key) {
        var header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
