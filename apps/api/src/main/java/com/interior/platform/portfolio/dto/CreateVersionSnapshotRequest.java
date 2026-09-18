package com.interior.platform.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVersionSnapshotRequest(
        @NotBlank(message = "Version snapshot label is required")
        @Size(max = 100, message = "Label must not exceed 100 characters")
        String label,

        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
