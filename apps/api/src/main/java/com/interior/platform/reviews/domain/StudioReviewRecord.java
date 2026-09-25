package com.interior.platform.reviews.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioReviewRecord(
        UUID id,
        UUID studioId,
        UUID leadId,
        UUID projectId,
        UUID reviewInvitationId,
        int rating,
        String title,
        String reviewText,
        String reviewerDisplayName,
        DisplayNameMode displayNameMode,
        ReviewStatus status,
        String studioResponseText,
        Instant studioResponseAt,
        Instant flaggedAt,
        Instant removedAt,
        Instant submittedAt,
        Instant publishedAt,
        Instant updatedAt,
        long version
) {
    public boolean isPubliclyVisible() {
        return status == ReviewStatus.PUBLISHED;
    }
}
