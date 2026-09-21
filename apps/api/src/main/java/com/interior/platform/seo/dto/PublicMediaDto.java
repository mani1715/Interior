package com.interior.platform.seo.dto;

import com.interior.platform.media.dto.MediaDerivativeDto;

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
        List<MediaDerivativeDto> derivatives
) {}
