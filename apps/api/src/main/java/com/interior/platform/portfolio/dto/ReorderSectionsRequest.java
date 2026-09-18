package com.interior.platform.portfolio.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderSectionsRequest(
        @NotEmpty(message = "Ordered section IDs list must not be empty")
        List<UUID> sectionIds,

        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
