package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank(message = "Comment text is required")
        @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
        String commentText
) {}
