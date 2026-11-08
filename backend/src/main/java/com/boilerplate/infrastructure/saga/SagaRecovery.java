package com.boilerplate.infrastructure.saga;

import com.boilerplate.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Re-drives saga instances that are still running but have not moved for
 * {@code app.saga.recovery-after-minutes} — the case where the process died between two
 * steps. Because every step is idempotent (payment keyed on the saga id, stock reservation
 * all-or-nothing, order transitions status-guarded) a re-drive is safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRecovery {

    private static final int BATCH = 50;

    private final SagaInstanceRepository sagaRepository;
    private final OrderFulfillmentSagaDriver driver;
    private final AppProperties appProperties;

    @Scheduled(fixedDelayString = "${app.saga.recovery-poll-ms:60000}")
    @SchedulerLock(name = "saga-recovery", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void recoverStalled() {
        Instant cutoff =
                Instant.now().minus(Duration.ofMinutes(appProperties.saga().recoveryAfterMinutes()));
        List<SagaInstance> stalled =
                sagaRepository.findByCompletedAtIsNullAndUpdatedAtBefore(cutoff, PageRequest.of(0, BATCH));
        for (SagaInstance saga : stalled) {
            if (OrderFulfillmentSaga.SAGA_TYPE.equals(saga.getSagaType())) {
                log.info(
                        "recovering stalled saga {} ({}) in state {}",
                        saga.getId(),
                        saga.getCorrelationId(),
                        saga.getState());
                driver.drive(saga.getCorrelationId());
            }
        }
    }
}
