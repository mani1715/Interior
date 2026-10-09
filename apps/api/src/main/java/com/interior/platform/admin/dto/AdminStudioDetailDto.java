package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminStudioDetailDto(
        UUID id,
        String name,
        String slug,
        UUID ownerId,
        String ownerEmail,
        String status,
        String suspensionReason,
        String publicationStatus,
        String verificationStatus,
        String planCode,
        long projectCount,
        long publicProjectCount,
        long photoCount,
        long storageBytes,
        long pendingUploadIntentsCount,
        long failedCommunicationCount,
        Instant createdAt
) {}
