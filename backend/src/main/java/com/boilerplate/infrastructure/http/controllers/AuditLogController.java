package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.audit.AuditLogQueryService;
import com.boilerplate.application.audit.AuditLogResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/audit")
@RequiredArgsConstructor
@Tag(name = "Audit log", description = "Append-only trail of who did what")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ResponseEntity<PagedResponse<AuditLogResponse>> forUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogQueryService.listByActor(userId.toString(), page, size));
    }
}
