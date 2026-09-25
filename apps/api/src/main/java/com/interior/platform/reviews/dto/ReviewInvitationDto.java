package com.interior.platform.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewInvitationDto(
        UUID id,
        UUID studioId,
        UUID leadId,
        UUID projectId,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant usedAt,
        Instant revokedAt
) {
}
