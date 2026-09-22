package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitClientCommentRequest(
        UUID jobId,

        @NotBlank(message = "Author name is required")
        @Size(max = 100, message = "Author name cannot exceed 100 characters")
        String authorName,

        @NotBlank(message = "Comment text is required")
        @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
        String commentText
) {}
