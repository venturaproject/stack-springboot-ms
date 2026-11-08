package com.boilerplate.application.notifications;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> data, long unreadCount) {}
