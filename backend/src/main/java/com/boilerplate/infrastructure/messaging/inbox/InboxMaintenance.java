package com.boilerplate.infrastructure.messaging.inbox;

import com.boilerplate.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps {@code inbox_processed_events} bounded. A dedupe row only has to outlive the
 * window in which a duplicate could still arrive (broker retention, consumer lag); after
 * {@code app.inbox.retention-days} it is safe to drop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InboxMaintenance {

    private final ProcessedEventRepository repository;
    private final AppProperties appProperties;

    @Scheduled(cron = "${app.inbox.purge-cron:0 40 3 * * *}")
    @SchedulerLock(name = "inbox-retention-purge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    @Transactional
    public void purgeOld() {
        Instant cutoff =
                Instant.now().minus(Duration.ofDays(appProperties.inbox().retentionDays()));
        int deleted = repository.deleteProcessedBefore(cutoff);
        if (deleted > 0) {
            log.info("inbox retention: purged {} dedupe rows older than {}", deleted, cutoff);
        }
    }
}
