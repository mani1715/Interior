package com.interior.platform.security.mfa;

import java.time.Instant;
import java.util.UUID;

public record RecoveryCodeRecord(
    UUID id,
    UUID userId,
    String codeHash,
    Instant usedAt,
    Instant createdAt
) {
    public boolean isUsed() {
        return usedAt != null;
    }
}
