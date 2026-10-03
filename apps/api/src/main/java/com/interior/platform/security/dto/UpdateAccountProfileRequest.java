package com.interior.platform.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileRequest(
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    String displayName,

    @Size(max = 25, message = "Phone must not exceed 25 characters")
    String phone,

    @Size(max = 1000, message = "Avatar URL must not exceed 1000 characters")
    String avatarUrl
) {}
