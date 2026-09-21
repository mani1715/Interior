package com.interior.platform.media.dto;

import java.util.UUID;

public record MediaPresentationDto(
        UUID id,
        UUID projectId,
        String mediaType,
        String thumbnailUrl,
        String mediumUrl,
        String largeUrl,
        boolean isCover,
        int sortOrder,
        String altText,
        String caption,
        int width,
        int height,
        boolean watermarked,
        boolean isAiConcept
) {}
