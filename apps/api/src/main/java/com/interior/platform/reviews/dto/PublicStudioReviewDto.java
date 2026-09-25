package com.interior.platform.reviews.dto;

import com.interior.platform.reviews.domain.DisplayNameMode;
import java.time.Instant;
import java.util.UUID;

public record PublicStudioReviewDto(
        UUID id,
        int rating,
        String title,
        String reviewText,
        String reviewerDisplayName,
        DisplayNameMode displayNameMode,
        Instant publishedAt,
        String studioResponseText,
        Instant studioResponseAt,
        UUID projectId,
        String projectTitle
) {
}
