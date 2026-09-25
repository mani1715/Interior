package com.interior.platform.reviews.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewInvitationRecord(
        UUID id,
        UUID studioId,
        UUID leadId,
        UUID projectId,
        byte[] tokenHash,
        ReviewInvitationStatus status,
        Instant expiresAt,
        UUID createdBy,
        Instant revokedAt,
        Instant usedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValidForSubmission() {
        return status == ReviewInvitationStatus.PENDING && !isExpired();
    }
}
