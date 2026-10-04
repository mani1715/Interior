package com.interior.platform.realtime.domain;

import com.interior.platform.common.util.UuidV7;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record RealtimeEvent(
        String eventId,
        RealtimeEventType type,
        Instant occurredAt,
        UUID recipientUserId,
        UUID studioId,
        String resourceType,
        String resourceId,
        String notificationId,
        Map<String, Object> metadata
) {
    public RealtimeEvent {
        if (eventId == null || eventId.isBlank()) {
            eventId = UuidV7.randomUuid().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
    }

    public static RealtimeEvent of(
            RealtimeEventType type,
            UUID recipientUserId,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata
    ) {
        return new RealtimeEvent(
                UuidV7.randomUuid().toString(),
                type,
                Instant.now(),
                recipientUserId,
                null,
                resourceType,
                resourceId,
                null,
                metadata
        );
    }

    public static RealtimeEvent ofStudio(
            RealtimeEventType type,
            UUID recipientUserId,
            UUID studioId,
            String resourceType,
            String resourceId,
            Map<String, Object> metadata
    ) {
        return new RealtimeEvent(
                UuidV7.randomUuid().toString(),
                type,
                Instant.now(),
                recipientUserId,
                studioId,
                resourceType,
                resourceId,
                null,
                metadata
        );
    }

    public static RealtimeEvent ofNotification(
            RealtimeEventType type,
            UUID recipientUserId,
            UUID studioId,
            UUID notificationId,
            Map<String, Object> metadata
    ) {
        return new RealtimeEvent(
                UuidV7.randomUuid().toString(),
                type,
                Instant.now(),
                recipientUserId,
                studioId,
                "NOTIFICATION",
                notificationId != null ? notificationId.toString() : null,
                notificationId != null ? notificationId.toString() : null,
                metadata
        );
    }
}
