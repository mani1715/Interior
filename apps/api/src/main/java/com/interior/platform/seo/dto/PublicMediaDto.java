package com.interior.platform.seo.dto;

import com.interior.platform.media.dto.MediaDerivativeDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicMediaDto(
        UUID id,
        String mediaType,
        String mediaTypeDisplayName,
        boolean isCover,
        String altText,
        String caption,
        boolean isAiConcept,
        List<MediaDerivativeDto> derivatives,
        UUID roomId,
        boolean isRoomCover,
        BigDecimal focalX,
        BigDecimal focalY,
        boolean motionEnabled,
        int sortOrder,
        int width,
        int height
) {
    public PublicMediaDto(
            UUID id,
            String mediaType,
            String mediaTypeDisplayName,
            boolean isCover,
            String altText,
            String caption,
            boolean isAiConcept,
            List<MediaDerivativeDto> derivatives
    ) {
        this(
                id,
                mediaType,
                mediaTypeDisplayName,
                isCover,
                altText,
                caption,
                isAiConcept,
                derivatives,
                null,
                false,
                new BigDecimal("0.50"),
                new BigDecimal("0.50"),
                true,
                0,
                0,
                0
        );
    }
}
