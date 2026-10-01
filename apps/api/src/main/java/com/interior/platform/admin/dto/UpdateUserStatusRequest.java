package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|SUSPENDED|DELETED", message = "Status must be ACTIVE, SUSPENDED, or DELETED")
        String status,

        String reason
) {}
