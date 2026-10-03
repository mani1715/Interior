package com.interior.platform.notifications.dto;

import java.util.List;

public record NotificationListResponse(
    List<NotificationDto> notifications,
    long unreadCount,
    int limit,
    int offset
) {}
