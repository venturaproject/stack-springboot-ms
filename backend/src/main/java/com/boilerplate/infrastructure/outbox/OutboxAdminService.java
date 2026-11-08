package com.boilerplate.infrastructure.outbox;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operator surface for the outbox dead-letter queue: list the rows the relay gave up on
 * and, once the cause is fixed, hand one back to the relay. Infrastructure — the outbox has
 * no domain of its own.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxAdminService {

    private final OutboxEventRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<OutboxEventView> deadLetters(int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page - 1), size);
        return PagedResponse.from(
                repository.findByFailedAtIsNotNullOrderByOccurredAtDesc(pageable), OutboxEventView::from);
    }

    @Transactional
    public void replay(UUID id) {
        OutboxEvent row =
                repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Outbox event not found: " + id));
        row.replay();
        repository.save(row);
        log.info("outbox event {} ({}) queued for replay by operator", id, row.getEventType());
    }
}
