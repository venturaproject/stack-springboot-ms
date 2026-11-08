package com.boilerplate.infrastructure.messaging;

import com.boilerplate.application.shared.EventPublisher;
import com.boilerplate.application.shared.PublishedEvent;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * {@link EventPublisher} adapter for Kafka. One topic per aggregate type
 * ({@code <prefix>.orders}, {@code <prefix>.products}, …). The message <b>key is the
 * aggregate id</b>, so every event of one aggregate lands on the same partition and is
 * delivered in order. The unique per-event id travels as the {@code event-id} header (the
 * consumer's dedupe key — see {@code InboxGuard}), and the stable event type as the
 * {@code event-type} header.
 *
 * <p>The send is awaited so a broker failure propagates as an exception — the outbox relay
 * then leaves the row unpublished and retries with backoff on a later tick.
 */
@Component
@Profile("kafka")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> eventKafkaTemplate;
    private final String topicPrefix;

    KafkaEventPublisher(
            KafkaTemplate<String, String> eventKafkaTemplate,
            @Value("${app.events.kafka.topic-prefix:boilerplate}") String topicPrefix) {
        this.eventKafkaTemplate = eventKafkaTemplate;
        this.topicPrefix = topicPrefix;
    }

    @Override
    public void publish(PublishedEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topicPrefix + "." + event.aggregateType(), event.aggregateId().toString(), event.payloadJson());
        record.headers().add(new RecordHeader("event-id", event.id().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("event-type", event.type().getBytes(StandardCharsets.UTF_8)));
        try {
            eventKafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted publishing " + event.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka publish failed for " + event.id(), e);
        }
    }
}
