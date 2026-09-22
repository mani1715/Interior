package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeReviewTokenRequest(
        @NotBlank(message = "Review token is required")
        String token
) {}
