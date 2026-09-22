package com.interior.platform.ai.dto;

import java.util.UUID;

public record PublicReviewItemDto(
        UUID id,
        UUID jobId,
        UUID mediaId,
        String displayLabel,
        int displayOrder,
        String mediaPreviewUrl,
        String currentDecision
) {}
