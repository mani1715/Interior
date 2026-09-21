package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAiJobRequest(
        @NotNull(message = "inputMediaId is required")
        UUID inputMediaId,

        @NotNull(message = "projectId is required")
        UUID projectId,

        @NotBlank(message = "Prompt cannot be blank")
        @Size(min = 5, max = 500, message = "Prompt must be between 5 and 500 characters")
        @Pattern(regexp = "^[^<>]*$", message = "Prompt must be plain text without HTML tags")
        String prompt,

        @Size(max = 128, message = "Idempotency key must not exceed 128 characters")
        String idempotencyKey
) {}
