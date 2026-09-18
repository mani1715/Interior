package com.interior.platform.portfolio.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateSectionRequest(
        Boolean isVisible,
        JsonNode content,
        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
