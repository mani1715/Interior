package com.interior.platform.reviews.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewInvitationSessionRecord(
        UUID id,
        UUID invitationId,
        UUID studioId,
        byte[] sessionTokenHash,
        byte[] csrfTokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isActive() {
        return revokedAt == null && expiresAt != null && Instant.now().isBefore(expiresAt);
    }
}
