package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminVerificationSummaryDto(
        UUID id,
        UUID studioId,
        String studioName,
        String studioSlug,
        String businessName,
        String professionalType,
        String status,
        String registrationNumber,
        String gstNumber,
        String websiteDomain,
        String notes,
        String decisionReason,
        Instant verifiedAt,
        Instant expiresAt,
        Instant submittedAt,
        int documentCount
) {}
