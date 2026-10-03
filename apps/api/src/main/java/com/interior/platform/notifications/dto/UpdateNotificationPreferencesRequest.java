package com.interior.platform.notifications.dto;

public record UpdateNotificationPreferencesRequest(
    Boolean inAppEnabled,
    Boolean emailEnabled,
    Boolean whatsappEnabled,
    Boolean leadNotifications,
    Boolean reviewNotifications,
    Boolean aiNotifications,
    Boolean systemNotifications
) {}
