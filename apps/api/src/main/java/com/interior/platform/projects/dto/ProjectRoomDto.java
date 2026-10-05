package com.interior.platform.projects.dto;

import com.interior.platform.projects.domain.RoomType;

import java.time.Instant;
import java.util.UUID;

public record ProjectRoomDto(
        UUID id,
        UUID studioId,
        UUID projectId,
        RoomType roomType,
        String roomTypeDisplayName,
        String displayName,
        int sortOrder,
        int photoCount,
        UUID coverMediaId,
        String coverMediaUrl,
        Instant createdAt,
        Instant updatedAt
) {}
