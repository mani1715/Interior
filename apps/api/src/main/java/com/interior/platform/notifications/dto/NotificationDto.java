package com.interior.platform.notifications.dto;

import com.interior.platform.notifications.domain.NotificationRecord;
import com.interior.platform.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
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
    public static NotificationDto from(NotificationRecord record) {
        return new NotificationDto(
            record.id(),
            record.userId(),
            record.studioId(),
            record.type(),
            record.title(),
            record.message(),
            record.actionUrl(),
            record.readAt(),
            record.createdAt(),
            record.metadata()
        );
    }
}
