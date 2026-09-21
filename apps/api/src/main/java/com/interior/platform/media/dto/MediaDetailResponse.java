package com.interior.platform.media.dto;

import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MediaDetailResponse(
        UUID id,
        UUID studioId,
        UUID projectId,
        MediaType mediaType,
        String mediaTypeDisplayName,
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
        List<MediaDerivativeDto> derivatives,
        Instant createdAt,
        Instant updatedAt
) {}
