package com.interior.platform.notifications.domain;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferencesRecord(
    UUID userId,
    boolean inAppEnabled,
    boolean emailEnabled,
    boolean whatsappEnabled,
    boolean leadNotifications,
    boolean reviewNotifications,
    boolean aiNotifications,
    boolean systemNotifications,
    Instant updatedAt
) {
    public static NotificationPreferencesRecord defaultForUser(UUID userId) {
        return new NotificationPreferencesRecord(
            userId,
            true,
            false,
            false,
            true,
            true,
            true,
            true,
            Instant.now()
        );
    }
}
