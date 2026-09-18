package com.interior.platform.portfolio.domain;

import java.time.Instant;
import java.util.UUID;

public record PortfolioVersionRecord(
        UUID id,
        UUID portfolioId,
        UUID studioId,
        int versionNumber,
        String label,
        String snapshotPayload,
        UUID createdBy,
        Instant createdAt
) {}
