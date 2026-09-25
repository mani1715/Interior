package com.interior.platform.reviews.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeReviewTokenRequest(
        @NotBlank(message = "Review invitation token is required")
        String token
) {
}
