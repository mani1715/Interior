package com.interior.platform.security.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionRecord(
    UUID id,
    UUID userId,
    byte[] tokenHash,
    byte[] csrfHash,
    Instant authTime,
    String assurance,
    Instant lastSeenAt,
    Instant idleExpiresAt,
    Instant absoluteExpiresAt,
    Instant revokedAt,
    String deviceLabel,
    Instant mfaVerifiedAt
) {
    public SessionRecord(
        UUID id,
        UUID userId,
        byte[] tokenHash,
        byte[] csrfHash,
        Instant authTime,
        String assurance,
        Instant lastSeenAt,
        Instant idleExpiresAt,
        Instant absoluteExpiresAt,
        Instant revokedAt,
        String deviceLabel
    ) {
        this(id, userId, tokenHash, csrfHash, authTime, assurance, lastSeenAt, idleExpiresAt, absoluteExpiresAt, revokedAt, deviceLabel, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(idleExpiresAt) || now.isAfter(absoluteExpiresAt);
    }

    public boolean isMfaVerified() {
        return mfaVerifiedAt != null || "MFA".equalsIgnoreCase(assurance) || "MFA_VERIFIED".equalsIgnoreCase(assurance);
    }
}
