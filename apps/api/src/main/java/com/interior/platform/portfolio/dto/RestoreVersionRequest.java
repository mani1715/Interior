package com.interior.platform.portfolio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RestoreVersionRequest(
        @Min(value = 1, message = "Target version number must be positive")
        int targetVersionNumber,

        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
