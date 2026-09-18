package com.interior.platform.portfolio.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.interior.platform.portfolio.domain.SectionType;

import java.time.Instant;
import java.util.UUID;

public record PortfolioSectionDto(
        UUID id,
        SectionType sectionType,
        int displayOrder,
        boolean isVisible,
        int schemaVersion,
        JsonNode content,
        Instant updatedAt
) {}
