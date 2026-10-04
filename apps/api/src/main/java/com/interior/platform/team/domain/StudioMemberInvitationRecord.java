package com.interior.platform.team.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioMemberInvitationRecord(
        UUID id,
        UUID studioId,
        String invitedEmail,
        String role,
        byte[] tokenHash,
        UUID invitedByUserId,
        String status,
        Instant expiresAt,
        Instant acceptedAt,
        UUID acceptedByUserId,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isPending(Instant now) {
        return "PENDING".equalsIgnoreCase(status)
                && revokedAt == null
                && acceptedAt == null
                && expiresAt != null
                && expiresAt.isAfter(now);
    }
}
