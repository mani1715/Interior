package com.interior.platform.portfolio.dto;

import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioDetailResponse(
        UUID id,
        UUID studioId,
        PortfolioTemplateKey templateKey,
        PortfolioStatus status,
        String headline,
        String subheadline,
        String bio,
        String designPhilosophy,
        Integer yearsOfExperience,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        FontPairing fontPairing,
        long version,
        boolean isReadyForPublish,
        List<String> readinessMissingRequirements,
        List<PortfolioSectionDto> sections,
        List<PortfolioVersionDto> recentVersions,
        Instant createdAt,
        Instant updatedAt
) {}
