package com.interior.platform.notifications.domain;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(
    UUID id,
    UUID userId,
    UUID studioId,
    NotificationType type,
    String title,
    String message,
    String actionUrl,
    Instant readAt,
    Instant createdAt,
    String metadata
) {
    public boolean isRead() {
        return readAt != null;
    }
}
