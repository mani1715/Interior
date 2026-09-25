package com.interior.platform.reviews.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewReportRecord(
        UUID id,
        UUID reviewId,
        UUID studioId,
        UUID reporterUserId,
        String reporterIp,
        ReportReason reason,
        String details,
        ReportStatus status,
        Instant createdAt
) {
}
