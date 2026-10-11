package com.interior.platform.support.dto;

import com.interior.platform.support.domain.SupportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSupportRequest(
        @NotNull(message = "Category is required")
        SupportCategory category,

        @NotBlank(message = "Subject cannot be blank")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        String subject,

        @NotBlank(message = "Message cannot be blank")
        @Size(max = 4000, message = "Message cannot exceed 4000 characters")
        String message,

        UUID studioId
) {}
