package com.interior.platform.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlatformFeedbackRequest(
    @NotBlank(message = "Category is required")
    String category,

    @NotBlank(message = "Feedback message is required")
    @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
    String message,

    @Size(max = 255, message = "Email must not exceed 255 characters")
    String contactEmail
) {}
