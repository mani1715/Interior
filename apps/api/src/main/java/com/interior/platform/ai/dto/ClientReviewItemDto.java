package com.interior.platform.ai.dto;

import java.util.UUID;

public record ClientReviewItemDto(
        UUID id,
        UUID jobId,
        UUID mediaId,
        String displayLabel,
        int displayOrder,
        String previewUrl
) {}
