package com.boilerplate.application.notifications;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.boilerplate.domain.exception.EntityNotFoundException;
import com.boilerplate.domain.notifications.Notification;
import com.boilerplate.domain.notifications.NotificationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    @Test
    void mark_read_only_loads_a_notification_owned_by_the_caller() {
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Notification notification = Notification.create(ownerId, "Title", "Message", "info");
        when(repository.findByIdAndUserId(notificationId, ownerId)).thenReturn(Optional.of(notification));

        service.markRead(notificationId, ownerId);

        verify(repository).save(notification);
    }

    @Test
    void another_users_or_broadcast_notification_cannot_be_mutated() {
        UUID notificationId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(repository.findByIdAndUserId(notificationId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(notificationId, callerId)).isInstanceOf(EntityNotFoundException.class);
        verify(repository).findByIdAndUserId(notificationId, callerId);
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
