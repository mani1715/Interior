package com.interior.platform.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ExchangeReviewSessionResult(
        UUID reviewPublicId,
        String sessionToken,
        String csrfToken,
        Instant expiresAt
) {}
