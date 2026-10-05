package com.interior.platform.projects.domain;

import java.time.Instant;
import java.util.UUID;

public record ProjectRoomRecord(
        UUID id,
        UUID studioId,
        UUID projectId,
        RoomType roomType,
        String displayName,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
