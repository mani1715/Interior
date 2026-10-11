package com.interior.platform.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportMessageRequest(
        @NotBlank(message = "Message body cannot be blank")
        @Size(max = 4000, message = "Message body cannot exceed 4000 characters")
        String message,

        Boolean isInternal
) {}
