package com.interior.platform.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudioRequest(
        @NotBlank(message = "Studio name is required")
        @Size(min = 2, max = 100, message = "Studio name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Professional type is required")
        String professionalType,

        @NotBlank(message = "City is required")
        @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(min = 2, max = 50, message = "State must be between 2 and 50 characters")
        String state,

        @Size(max = 64, message = "Slug cannot exceed 64 characters")
        String slug
) {
}
