package com.interior.platform.notifications.dto;

import com.interior.platform.notifications.domain.NotificationPreferencesRecord;

import java.util.UUID;

public record NotificationPreferencesDto(
    UUID userId,
    boolean inAppEnabled,
    boolean emailEnabled,
    boolean whatsappEnabled,
    boolean leadNotifications,
    boolean reviewNotifications,
    boolean aiNotifications,
    boolean systemNotifications
) {
    public static NotificationPreferencesDto from(NotificationPreferencesRecord record) {
        return new NotificationPreferencesDto(
            record.userId(),
            record.inAppEnabled(),
            record.emailEnabled(),
            record.whatsappEnabled(),
            record.leadNotifications(),
            record.reviewNotifications(),
            record.aiNotifications(),
            record.systemNotifications()
        );
    }
}
