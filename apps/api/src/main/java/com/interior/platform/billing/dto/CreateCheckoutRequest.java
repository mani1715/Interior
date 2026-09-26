package com.interior.platform.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutRequest(
        @NotBlank(message = "Plan code is required")
        String planCode,
        String successUrl,
        String cancelUrl
) {}
