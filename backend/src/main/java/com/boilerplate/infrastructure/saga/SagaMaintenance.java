package com.boilerplate.infrastructure.saga;

import com.boilerplate.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Drops saga instances that finished more than {@code app.saga.retention-days} ago. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaMaintenance {

    private final SagaInstanceRepository sagaRepository;
    private final AppProperties appProperties;

    @Scheduled(cron = "${app.saga.purge-cron:0 50 3 * * *}")
    @SchedulerLock(name = "saga-retention-purge", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    @Transactional
    public void purgeCompleted() {
        Instant cutoff =
                Instant.now().minus(Duration.ofDays(appProperties.saga().retentionDays()));
        int deleted = sagaRepository.deleteCompletedBefore(cutoff);
        if (deleted > 0) {
            log.info("saga retention: purged {} finished instances older than {}", deleted, cutoff);
        }
    }
}
