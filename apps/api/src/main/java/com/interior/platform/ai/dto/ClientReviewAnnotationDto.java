package com.interior.platform.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientReviewAnnotationDto(
        UUID id,
        UUID reviewId,
        UUID jobId,
        UUID mediaId,
        int pinNumber,
        double coordX,
        double coordY,
        String authorType,
        String authorName,
        String commentText,
        boolean isChangeRequest,
        Instant resolvedAt,
        String resolvedBy,
        UUID parentAnnotationId,
        int revisionRound,
        Instant createdAt
) {}
