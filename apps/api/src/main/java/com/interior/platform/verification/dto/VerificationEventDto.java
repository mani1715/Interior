package com.interior.platform.verification.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificationEventDto(
        UUID id,
        String eventType,
        UUID actorUserId,
        String reason,
        Instant createdAt
) {
}
