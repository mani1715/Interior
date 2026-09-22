package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewRecord(
        UUID id,
        UUID studioId,
        UUID projectId,
        String title,
        String customMessage,
        byte[] tokenHash,
        ClientReviewStatus status,
        boolean includeOriginal,
        Instant expiresAt,
        UUID currentApprovedJobId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isOperable(Instant now) {
        return status == ClientReviewStatus.OPEN && !isExpired(now);
    }
}
