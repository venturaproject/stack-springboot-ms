package com.boilerplate.infrastructure.outbox;

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
 * Keeps {@code outbox_events} from growing without bound: once a row is published it has
 * done its job, so after {@code app.outbox.retention-days} it is deleted. Dead-lettered
 * rows ({@code failed_at} set) are left alone — they need a human.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxMaintenance {

    private final OutboxEventRepository repository;
    private final AppProperties appProperties;

    @Scheduled(cron = "${app.outbox.purge-cron:0 30 3 * * *}")
    @SchedulerLock(name = "outbox-retention-purge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    @Transactional
    public void purgePublished() {
        Instant cutoff =
                Instant.now().minus(Duration.ofDays(appProperties.outbox().retentionDays()));
        int deleted = repository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("outbox retention: purged {} published rows older than {}", deleted, cutoff);
        }
    }
}
