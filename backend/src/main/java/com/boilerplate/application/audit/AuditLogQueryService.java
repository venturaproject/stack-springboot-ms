package com.boilerplate.application.audit;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.domain.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> listByActor(String actorId, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page - 1), size);
        return PagedResponse.from(
                auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable), AuditLogResponse::from);
    }
}
