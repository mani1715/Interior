package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminReviewSummaryDto(
        UUID id,
        UUID studioId,
        String studioName,
        UUID leadId,
        int rating,
        String title,
        String reviewText,
        String reviewerDisplayName,
        String displayNameMode,
        String status,
        String studioResponseText,
        Instant studioResponseAt,
        Instant submittedAt,
        Instant publishedAt,
        int reportCount
) {}
