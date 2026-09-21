package com.interior.platform.seo.dto;

import com.interior.platform.projects.dto.ProjectPresentationDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicProjectDetailDto(
        UUID id,
        String slug,
        String title,
        String shortDescription,
        String fullDescription,
        String categoryCode,
        String categoryDisplayName,
        List<String> styleCodes,
        List<String> styleDisplayNames,
        String city,
        String district,
        String state,
        String country,
        String propertyType,
        String projectScope,
        Integer completionYear,
        String budgetFormatted,
        String areaFormatted,
        Instant updatedAt,
        String metaTitle,
        String metaDescription,
        String canonicalUrl,
        PublicStudioSummaryDto studio,
        List<PublicMediaDto> media,
        List<ProjectPresentationDto> relatedProjects
) {}
