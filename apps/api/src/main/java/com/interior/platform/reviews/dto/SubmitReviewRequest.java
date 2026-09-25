package com.interior.platform.reviews.dto;

import com.interior.platform.reviews.domain.DisplayNameMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitReviewRequest(
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        int rating,

        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Review text is required")
        @Size(min = 10, max = 2000, message = "Review text must be between 10 and 2000 characters")
        String reviewText,

        @NotNull(message = "Display name mode is required")
        DisplayNameMode displayNameMode,

        @Size(max = 100, message = "Custom display name cannot exceed 100 characters")
        String customDisplayName
) {
}
