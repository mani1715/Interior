package com.interior.platform.media.domain;

import java.math.BigDecimal;
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
        Instant deletedAt,
        UUID roomId,
        boolean isRoomCover,
        BigDecimal focalX,
        BigDecimal focalY,
        boolean motionEnabled
) {
    public MediaAssetRecord(
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
    ) {
        this(
                id,
                studioId,
                projectId,
                mediaType,
                visibility,
                processingStatus,
                originalStorageKey,
                contentType,
                fileSize,
                width,
                height,
                sortOrder,
                isCover,
                altText,
                caption,
                watermarkEnabled,
                createdBy,
                createdAt,
                updatedAt,
                deletedAt,
                null,
                false,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                true
        );
    }
}
