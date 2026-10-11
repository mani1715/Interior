package com.interior.platform.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record DowngradePlanRequest(
        @NotBlank(message = "Target plan code is required")
        String targetPlanCode
) {}
