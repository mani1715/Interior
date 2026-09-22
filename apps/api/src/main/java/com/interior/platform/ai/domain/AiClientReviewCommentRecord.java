package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiClientReviewCommentRecord(
        UUID id,
        UUID reviewId,
        UUID studioId,
        UUID jobId,
        CommentAuthorType authorType,
        String authorName,
        String commentText,
        Instant createdAt
) {}
