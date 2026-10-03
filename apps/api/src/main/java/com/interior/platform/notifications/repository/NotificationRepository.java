package com.interior.platform.notifications.repository;

import com.interior.platform.notifications.domain.NotificationPreferencesRecord;
import com.interior.platform.notifications.domain.NotificationRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    void createNotification(NotificationRecord notification);
    List<NotificationRecord> findByUserId(UUID userId, int limit, int offset);
    long countUnreadByUserId(UUID userId);
    Optional<NotificationRecord> findByIdAndUserId(UUID id, UUID userId);
    void markAsRead(UUID id, UUID userId, Instant readAt);
    void markAllAsRead(UUID userId, Instant readAt);
    NotificationPreferencesRecord getPreferences(UUID userId);
    void savePreferences(NotificationPreferencesRecord preferences);
}
