package com.boilerplate.application.notifications;

import com.boilerplate.domain.notifications.Notification;
import com.boilerplate.domain.notifications.NotificationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private static final int MAX_ITEMS = 50;

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse listFor(UUID userId) {
        List<Notification> items = notificationRepository.findVisibleTo(userId, PageRequest.of(0, MAX_ITEMS));
        long unread = notificationRepository.countUnreadFor(userId);
        return new NotificationListResponse(
                items.stream().map(NotificationResponse::from).toList(), unread);
    }
}
