package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStudioStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "Status must be ACTIVE or SUSPENDED")
        String status,

        String reason
) {}
