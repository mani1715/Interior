package com.interior.platform.collections.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SaveProjectRequest(
        @NotNull(message = "Project ID is required")
        UUID projectId,

        UUID collectionId,

        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {
}
