package com.interior.platform.projects.dto;

import java.util.List;
import java.util.UUID;

public record ProjectPresentationDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String fullDescription,
        String categoryCode,
        String categoryDisplayName,
        List<String> styleCodes,
        List<String> styleDisplayNames,
        String location,
        String propertyType,
        String projectScope,
        Integer completionYear,
        boolean featured,
        int displayOrder,
        String clientName,
        String budgetFormatted,
        String areaFormatted,
        String coverImageUrl
) {}
