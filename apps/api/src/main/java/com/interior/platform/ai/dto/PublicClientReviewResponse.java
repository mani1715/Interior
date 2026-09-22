package com.interior.platform.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicClientReviewResponse(
        UUID reviewPublicId,
        String studioName,
        String projectTitle,
        String title,
        String customMessage,
        String status,
        boolean includeOriginal,
        String originalMediaPreviewUrl,
        Instant expiresAt,
        boolean isExpired,
        UUID currentApprovedJobId,
        List<PublicReviewItemDto> items,
        List<PublicReviewDecisionDto> recentDecisions,
        List<PublicReviewCommentDto> comments,
        Instant createdAt
) {}
