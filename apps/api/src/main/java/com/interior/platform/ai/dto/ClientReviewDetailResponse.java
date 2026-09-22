package com.interior.platform.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientReviewDetailResponse(
        UUID id,
        UUID projectId,
        String projectTitle,
        String title,
        String customMessage,
        String status,
        boolean includeOriginal,
        Instant expiresAt,
        UUID currentApprovedJobId,
        List<ClientReviewItemDto> items,
        List<ClientReviewDecisionDto> decisions,
        List<ClientReviewCommentDto> comments,
        Instant createdAt,
        Instant updatedAt
) {}
