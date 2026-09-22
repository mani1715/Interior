package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewSessionRecord(
        UUID id,
        UUID reviewId,
        byte[] sessionTokenHash,
        byte[] csrfTokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}
