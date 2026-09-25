package com.interior.platform.reviews.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateReviewInvitationResponse(
        UUID invitationId,
        String rawToken,
        String invitationUrl,
        Instant expiresAt
) {
}
