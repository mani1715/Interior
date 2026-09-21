package com.interior.platform.seo.dto;

import com.interior.platform.projects.dto.ProjectPresentationDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicStudioDto(
        UUID id,
        String slug,
        String name,
        String professionalType,
        String professionalTitle,
        String tagline,
        Integer experienceSinceYear,
        String city,
        String district,
        String state,
        String country,
        Instant publishedAt,
        String metaTitle,
        String metaDescription,
        String canonicalUrl,
        boolean indexingEnabled,
        List<PublicContactDto> contacts,
        List<String> services,
        List<String> specialties,
        List<String> serviceAreas,
        PublicPortfolioDto portfolio,
        List<ProjectPresentationDto> projects
) {}
