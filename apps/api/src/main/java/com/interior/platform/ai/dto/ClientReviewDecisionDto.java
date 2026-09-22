package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ClientReviewDecisionType;

import java.time.Instant;
import java.util.UUID;

public record ClientReviewDecisionDto(
        UUID id,
        UUID jobId,
        ClientReviewDecisionType decision,
        String clientName,
        String feedback,
        boolean isCurrent,
        Instant createdAt
) {}
