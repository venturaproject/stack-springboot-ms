package com.boilerplate.application.shared;

/**
 * Outbound port for delivering domain events past the system boundary. The default adapter
 * ({@code infrastructure.messaging.LoggingEventPublisher}) just logs; swap in
 * {@code KafkaEventPublisher} with the {@code kafka} profile.
 */
public interface EventPublisher {

    void publish(PublishedEvent event);
}
