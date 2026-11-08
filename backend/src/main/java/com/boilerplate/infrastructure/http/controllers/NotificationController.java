package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.notifications.NotificationListResponse;
import com.boilerplate.application.notifications.NotificationQueryService;
import com.boilerplate.application.notifications.NotificationResponse;
import com.boilerplate.application.notifications.NotificationService;
import com.boilerplate.application.notifications.SendNotificationCommand;
import com.boilerplate.infrastructure.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Per-user notification inbox")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public ResponseEntity<NotificationListResponse> list(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(notificationQueryService.listFor(principal.id()));
    }

    @PostMapping
    @Operation(summary = "Send a notification to the current user (demo affordance)")
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody SendNotificationCommand command) {
        NotificationResponse created =
                notificationService.create(principal.id(), command.title(), command.message(), command.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal SecurityUser principal) {
        notificationService.markAllRead(principal.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal SecurityUser principal) {
        notificationService.deleteAll(principal.id());
        return ResponseEntity.noContent().build();
    }
}
