package com.boilerplate.infrastructure.saga;

import com.boilerplate.config.AppProperties;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Meters the saga engine: {@code saga.running} (instances mid-flight) and {@code saga.stalled}
 * (running but untouched past {@code app.saga.recovery-after-minutes} — i.e. the recovery job
 * has work to do). A steadily climbing {@code saga.stalled} means compensation or a step is
 * wedged. Scrape from {@code /actuator/prometheus}.
 */
@Component
@RequiredArgsConstructor
class SagaObservability implements MeterBinder {

    private final SagaInstanceRepository repository;
    private final AppProperties appProperties;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("saga.running", repository, SagaInstanceRepository::countByCompletedAtIsNull)
                .description("Saga instances that have not reached a terminal state")
                .register(registry);
        Gauge.builder("saga.stalled", this, SagaObservability::stalledCount)
                .description("Running saga instances the recovery job considers stalled")
                .register(registry);
    }

    private double stalledCount() {
        Instant cutoff =
                Instant.now().minus(Duration.ofMinutes(appProperties.saga().recoveryAfterMinutes()));
        return repository.countByCompletedAtIsNullAndUpdatedAtBefore(cutoff);
    }
}
