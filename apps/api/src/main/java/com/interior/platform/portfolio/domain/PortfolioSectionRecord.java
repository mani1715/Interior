package com.interior.platform.portfolio.domain;

import java.time.Instant;
import java.util.UUID;

public record PortfolioSectionRecord(
        UUID id,
        UUID portfolioId,
        UUID studioId,
        SectionType sectionType,
        int displayOrder,
        boolean isVisible,
        int schemaVersion,
        String content,
        Instant createdAt,
        Instant updatedAt
) {}
