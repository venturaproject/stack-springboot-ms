package com.boilerplate.infrastructure.saga;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface SagaInstanceRepository extends Repository<SagaInstance, UUID> {

    /**
     * Idempotent start: returns {@code 1} if this call created the instance, {@code 0} if a
     * row for {@code (sagaType, correlationId)} already existed (a redelivered trigger).
     */
    @Modifying
    @Query(
            value =
                    """
        INSERT INTO saga_instances (id, saga_type, correlation_id, state, payload)
        VALUES (:id, :sagaType, :correlationId, :state, :payload)
        ON CONFLICT (saga_type, correlation_id) DO NOTHING
        """,
            nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("sagaType") String sagaType,
            @Param("correlationId") UUID correlationId,
            @Param("state") String state,
            @Param("payload") String payload);

    Optional<SagaInstance> findBySagaTypeAndCorrelationId(String sagaType, UUID correlationId);

    SagaInstance save(SagaInstance instance);

    /** Recovery: instances still running whose last step committed before {@code cutoff}. */
    List<SagaInstance> findByCompletedAtIsNullAndUpdatedAtBefore(Instant cutoff, Pageable pageable);

    /** Metrics: how many saga instances are currently mid-flight. */
    long countByCompletedAtIsNull();

    /** Metrics/health: running instances that have not moved since {@code cutoff}. */
    long countByCompletedAtIsNullAndUpdatedAtBefore(Instant cutoff);

    /** Retention: drop instances that finished before {@code cutoff}. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SagaInstance s WHERE s.completedAt IS NOT NULL AND s.completedAt < :cutoff")
    int deleteCompletedBefore(@Param("cutoff") Instant cutoff);
}
