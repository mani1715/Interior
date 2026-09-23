package com.interior.platform.discovery.dto;

import java.util.List;
import java.util.UUID;

public record DiscoveryProjectCardDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String categoryCode,
        String categoryName,
        List<String> styleCodes,
        List<String> styleNames,
        String city,
        String state,
        String propertyType,
        String projectScope,
        String coverImageUrl,
        boolean isAiConceptCover,
        UUID studioId,
        String studioSlug,
        String studioName,
        String professionalType,
        String professionalTypeLabel,
        Integer completionYear
) {}
