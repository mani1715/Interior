package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStudioPlanRequest(
        @NotBlank(message = "Plan code is required")
        @Pattern(regexp = "STANDARD|PREMIUM|PRO|BASE", message = "Plan code must be STANDARD, PREMIUM, PRO, or BASE")
        String planCode
) {}
