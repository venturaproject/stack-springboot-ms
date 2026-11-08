package com.boilerplate.infrastructure.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Turns the outbox into something you can alert on.
 *
 * <ul>
 *   <li>Meters {@code outbox.pending} (staged, not yet published) and
 *       {@code outbox.dead_letters} (relay gave up) — scrape them from {@code /actuator/prometheus}.</li>
 *   <li>Contributes an {@code outbox} entry to {@code /actuator/health}: {@code DOWN} once the
 *       dead-letter count reaches {@code app.outbox.dead-letter-alert-threshold}. It is deliberately
 *       <em>not</em> in the {@code readiness}/{@code liveness} groups — a poison event should page a
 *       human, not make Kubernetes cycle the pod.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
class OutboxObservability implements MeterBinder, HealthIndicator {

    private final OutboxEventRepository repository;

    @Value("${app.outbox.dead-letter-alert-threshold:10}")
    private int deadLetterAlertThreshold;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("outbox.pending", repository, OutboxEventRepository::countByPublishedAtIsNullAndFailedAtIsNull)
                .description("Domain events staged in the outbox but not yet published")
                .register(registry);
        Gauge.builder("outbox.dead_letters", repository, OutboxEventRepository::countByFailedAtIsNotNull)
                .description("Outbox rows the relay dead-lettered after exhausting its retries")
                .register(registry);
    }

    @Override
    public Health health() {
        long deadLetters = repository.countByFailedAtIsNotNull();
        Health.Builder status = deadLetters >= deadLetterAlertThreshold ? Health.down() : Health.up();
        return status.withDetail("deadLetters", deadLetters)
                .withDetail("alertThreshold", deadLetterAlertThreshold)
                .build();
    }
}
