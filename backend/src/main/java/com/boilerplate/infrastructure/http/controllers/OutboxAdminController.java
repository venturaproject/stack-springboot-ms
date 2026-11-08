package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.infrastructure.outbox.OutboxAdminService;
import com.boilerplate.infrastructure.outbox.OutboxEventView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/outbox")
@RequiredArgsConstructor
@Tag(name = "Outbox admin", description = "Inspect and replay the transactional-outbox dead-letter queue")
public class OutboxAdminController {

    private final OutboxAdminService outboxAdminService;

    @GetMapping("/dead-letters")
    @PreAuthorize("hasAuthority('outbox:manage')")
    @Operation(summary = "List events the relay dead-lettered after exhausting its retries")
    public ResponseEntity<PagedResponse<OutboxEventView>> deadLetters(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(outboxAdminService.deadLetters(page, size));
    }

    @PostMapping("/dead-letters/{id}/replay")
    @PreAuthorize("hasAuthority('outbox:manage')")
    @Operation(summary = "Clear a dead-lettered event's failure state so the relay retries it")
    public ResponseEntity<Void> replay(@PathVariable UUID id) {
        outboxAdminService.replay(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
