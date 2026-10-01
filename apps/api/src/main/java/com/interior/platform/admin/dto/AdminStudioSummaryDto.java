package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminStudioSummaryDto(
        UUID id,
        String name,
        String slug,
        UUID ownerId,
        String ownerEmail,
        String status,
        String publicationStatus,
        String verificationStatus,
        long projectCount,
        long publicProjectCount,
        Instant createdAt
) {}
