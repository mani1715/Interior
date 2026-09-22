package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.CommentAuthorType;

import java.time.Instant;
import java.util.UUID;

public record PublicReviewCommentDto(
        UUID id,
        UUID jobId,
        CommentAuthorType authorType,
        String authorName,
        String commentText,
        Instant createdAt
) {}
