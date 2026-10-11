package com.interior.platform.privacy.domain;

import java.time.Instant;
import java.util.UUID;

public record PrivacyRequestRecord(
        UUID id,
        UUID userId,
        UUID studioId,
        PrivacyRequestType requestType,
        PrivacyRequestStatus status,
        Instant confirmedAt,
        Instant completedAt,
        String failureReason,
        String metadata,
        Instant createdAt,
        Instant updatedAt
) {}
