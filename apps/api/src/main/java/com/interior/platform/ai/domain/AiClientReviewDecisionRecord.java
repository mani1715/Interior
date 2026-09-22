package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewDecisionRecord(
        UUID id,
        UUID reviewId,
        UUID studioId,
        UUID jobId,
        ClientReviewDecisionType decision,
        String clientName,
        String feedback,
        boolean isCurrent,
        Instant createdAt
) {}
