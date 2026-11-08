package com.boilerplate.domain.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/** Persistence port for {@link AuditLog}. Append + read only. */
public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog auditLog);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);

    Page<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDesc(String entity, String entityId, Pageable pageable);

    long count();
}
