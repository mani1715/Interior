package com.interior.platform.reviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudioReviewResponseRequest(
        @NotBlank(message = "Response text is required")
        @Size(max = 1500, message = "Response text cannot exceed 1500 characters")
        String responseText
) {
}
