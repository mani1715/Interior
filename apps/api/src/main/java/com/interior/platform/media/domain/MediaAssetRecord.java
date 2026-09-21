package com.interior.platform.media.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetRecord(
        UUID id,
        UUID studioId,
        UUID projectId,
        MediaType mediaType,
        MediaVisibility visibility,
        MediaProcessingStatus processingStatus,
        String originalStorageKey,
        String contentType,
        long fileSize,
        int width,
        int height,
        int sortOrder,
        boolean isCover,
        String altText,
        String caption,
        boolean watermarkEnabled,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
