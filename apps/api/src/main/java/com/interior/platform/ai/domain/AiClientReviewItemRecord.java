package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewItemRecord(
        UUID id,
        UUID reviewId,
        UUID studioId,
        UUID jobId,
        UUID mediaId,
        String displayLabel,
        int displayOrder,
        Instant createdAt
) {}
