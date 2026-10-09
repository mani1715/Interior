package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminProjectSummaryDto(
        UUID id,
        UUID studioId,
        String studioName,
        String studioSlug,
        String title,
        String slug,
        String categoryCode,
        String projectStatus,
        String visibilityStatus,
        String moderationStatus,
        String moderationReason,
        Instant createdAt
) {}
