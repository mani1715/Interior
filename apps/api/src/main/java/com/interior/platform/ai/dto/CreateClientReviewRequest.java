package com.interior.platform.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateClientReviewRequest(
        @NotNull(message = "Project ID is required")
        UUID projectId,

        @NotBlank(message = "Review title is required")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @Size(max = 1000, message = "Custom message cannot exceed 1000 characters")
        String customMessage,

        @Min(value = 1, message = "Expiry must be at least 1 day")
        @Max(value = 60, message = "Expiry cannot exceed 60 days")
        Integer expiryDays,

        Boolean includeOriginal,

        @NotEmpty(message = "At least one concept must be selected")
        @Size(max = 10, message = "Cannot share more than 10 concepts in a single review")
        List<UUID> conceptJobIds
) {
    public int resolvedExpiryDays() {
        return expiryDays != null ? expiryDays : 7;
    }

    public boolean resolvedIncludeOriginal() {
        return Boolean.TRUE.equals(includeOriginal);
    }
}
