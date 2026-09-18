package com.interior.platform.portfolio.dto;

import java.time.Instant;
import java.util.UUID;

public record PortfolioVersionDto(
        UUID id,
        int versionNumber,
        String label,
        UUID createdBy,
        Instant createdAt
) {}
