package com.interior.platform.collections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCollectionRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}
