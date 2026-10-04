package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewAnnotationRecord(
        UUID id,
        UUID reviewId,
        UUID studioId,
        UUID jobId,
        UUID mediaId,
        int pinNumber,
        double coordX,
        double coordY,
        CommentAuthorType authorType,
        String authorName,
        String commentText,
        boolean isChangeRequest,
        Instant resolvedAt,
        String resolvedBy,
        UUID parentAnnotationId,
        int revisionRound,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public boolean isResolved() {
        return resolvedAt != null;
    }
}
