package com.boilerplate.application.notifications;

import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.notifications.Notification;
import com.boilerplate.domain.notifications.NotificationRepository;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse create(@Nullable UUID userId, String title, String message, @Nullable String type) {
        return NotificationResponse.from(
                notificationRepository.save(Notification.create(userId, title, message, type)));
    }

    @Transactional
    public void markRead(UUID id, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
        notification.markRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadFor(userId);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(UUID userId) {
        notificationRepository.deleteAllForUser(userId);
    }
}
