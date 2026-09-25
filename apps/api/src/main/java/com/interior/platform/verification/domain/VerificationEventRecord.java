package com.interior.platform.verification.domain;

import java.time.Instant;
import java.util.UUID;

public record VerificationEventRecord(
        UUID id,
        UUID verificationId,
        UUID studioId,
        VerificationEventType eventType,
        UUID actorUserId,
        String reason,
        String metadata,
        Instant createdAt
) {
}
