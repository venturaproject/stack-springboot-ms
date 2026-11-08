package com.boilerplate.infrastructure.outbox;

import com.boilerplate.application.shared.EventPublisher;
import com.boilerplate.application.shared.PublishedEvent;
import com.boilerplate.config.AppProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox and hands each staged event to the {@link EventPublisher}. Runs after
 * the producing transaction has committed, so publication side effects are never rolled
 * back. At-least-once: a publish that succeeds but fails to mark the row is retried, so
 * consumers must be idempotent (see {@code InboxGuard}). A failed publish is retried with
 * exponential backoff ({@code app.outbox.backoff.*}); after {@code app.outbox.max-attempts}
 * failures the row is dead-lettered ({@code failed_at} set) and skipped.
 */
@Component
@Slf4j
public class OutboxRelay {

    private static final int BATCH = 50;

    private final OutboxEventRepository repository;
    private final EventPublisher publisher;
    private final AppProperties appProperties;
    private final Counter published;
    private final Counter failed;

    public OutboxRelay(
            OutboxEventRepository repository,
            EventPublisher publisher,
            AppProperties appProperties,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.publisher = publisher;
        this.appProperties = appProperties;
        this.published = Counter.builder("outbox.published")
                .description("Domain events successfully handed to the EventPublisher")
                .register(meterRegistry);
        this.failed = Counter.builder("outbox.publish.failed")
                .description("Outbox publish attempts that threw")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:2000}")
    @Transactional
    public void drain() {
        List<OutboxEvent> batch = repository.claimDue(BATCH);
        if (batch.isEmpty()) {
            return;
        }
        AppProperties.Outbox cfg = appProperties.outbox();
        for (OutboxEvent row : batch) {
            try {
                publisher.publish(new PublishedEvent(
                        row.getId(),
                        row.getEventType(),
                        row.getAggregateType(),
                        row.getAggregateId(),
                        row.getPayload(),
                        row.getOccurredAt()));
                row.markPublished();
                published.increment();
            } catch (RuntimeException ex) {
                failed.increment();
                Duration wait = backoff(row.getAttempts(), cfg);
                row.recordFailure(wait);
                if (row.getAttempts() >= cfg.maxAttempts()) {
                    row.deadLetter();
                    log.error(
                            "outbox event {} ({}) dead-lettered after {} attempts",
                            row.getId(),
                            row.getEventType(),
                            row.getAttempts(),
                            ex);
                } else {
                    log.warn(
                            "outbox publish failed for {} (attempt {}/{}), retrying after {}",
                            row.getId(),
                            row.getAttempts(),
                            cfg.maxAttempts(),
                            wait,
                            ex);
                }
            }
            repository.save(row);
        }
    }

    /** {@code base * 2^attempts}, capped at {@code backoffMaxMs}. {@code attempts} is the count so far. */
    private static Duration backoff(int attempts, AppProperties.Outbox cfg) {
        int exp = Math.min(Math.max(attempts, 0), 20); // 2^20 ms ≈ 12 days — well past any sane cap
        long millis = Math.min(cfg.backoffBaseMs() << exp, cfg.backoffMaxMs());
        return Duration.ofMillis(millis);
    }
}
