package com.interior.platform.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivacyRequestResponse(
        UUID id,
        UUID userId,
        UUID studioId,
        String requestType,
        String status,
        Instant confirmedAt,
        Instant completedAt,
        String failureReason,
        Instant createdAt
) {}
