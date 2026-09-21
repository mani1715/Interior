package com.interior.platform.projects.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderProjectsRequest(
        @NotNull(message = "Ordered project IDs list cannot be null")
        @NotEmpty(message = "Ordered project IDs list cannot be empty")
        List<UUID> orderedProjectIds
) {}
