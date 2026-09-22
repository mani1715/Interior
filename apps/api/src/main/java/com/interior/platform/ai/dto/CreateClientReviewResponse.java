package com.interior.platform.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateClientReviewResponse(
        UUID id,
        UUID projectId,
        String title,
        String customMessage,
        String rawToken,
        String reviewUrl,
        String status,
        boolean includeOriginal,
        Instant expiresAt,
        List<ClientReviewItemDto> items,
        Instant createdAt
) {}
