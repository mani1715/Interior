package com.interior.platform.portfolio.domain;

import java.time.Instant;
import java.util.UUID;

public record PortfolioRecord(
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
        Instant createdAt,
        Instant updatedAt
) {}
