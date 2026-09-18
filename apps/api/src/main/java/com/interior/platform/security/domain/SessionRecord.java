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
    String deviceLabel
) {
    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(idleExpiresAt) || now.isAfter(absoluteExpiresAt);
    }
}
