package com.interior.platform.security.mfa;

import java.time.Instant;
import java.util.UUID;

public record MfaMethodRecord(
    UUID id,
    UUID userId,
    String methodType,
    String encryptedSecret,
    int keyVersion,
    String status,
    Instant pendingExpiresAt,
    Instant confirmedAt,
    Long lastUsedCounter,
    int failedAttempts,
    Instant lockedUntil,
    Instant createdAt,
    Instant updatedAt
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isPendingExpired(Instant now) {
        return isPending() && pendingExpiresAt != null && now.isAfter(pendingExpiresAt);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }
}
