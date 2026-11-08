package com.boilerplate.application.notifications;

import com.boilerplate.domain.notifications.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, String title, String message, String type, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getMessage(), n.getType(), n.isRead(), n.getCreatedAt());
    }
}
