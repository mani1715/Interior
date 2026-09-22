package com.interior.platform.ai.dto;

import java.util.UUID;

public record ExchangeReviewTokenResponse(
        UUID reviewPublicId,
        String csrfToken,
        String redirectUrl
) {}
