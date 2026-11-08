package com.boilerplate.infrastructure.messaging;

import com.boilerplate.application.shared.EventPublisher;
import com.boilerplate.application.shared.PublishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default {@link EventPublisher}: logs each event. Zero infrastructure — the boilerplate
 * runs with {@code docker compose up} and nothing else. Activate the {@code kafka} profile
 * to swap in {@link KafkaEventPublisher}.
 */
@Component
@Profile("!kafka")
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    @Override
    public void publish(PublishedEvent event) {
        log.info("[event] {}/{} {}", event.aggregateType(), event.type(), event.payloadJson());
    }
}
