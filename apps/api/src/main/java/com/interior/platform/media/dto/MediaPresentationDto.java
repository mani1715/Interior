package com.interior.platform.media.dto;

import java.math.BigDecimal;
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
        boolean isAiConcept,
        UUID roomId,
        boolean isRoomCover,
        BigDecimal focalX,
        BigDecimal focalY,
        boolean motionEnabled
) {
    public MediaPresentationDto(
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
    ) {
        this(
                id,
                projectId,
                mediaType,
                thumbnailUrl,
                mediumUrl,
                largeUrl,
                isCover,
                sortOrder,
                altText,
                caption,
                width,
                height,
                watermarked,
                isAiConcept,
                null,
                false,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                true
        );
    }
}
