package com.interior.platform.privacy.domain;

import java.time.Instant;
import java.util.UUID;

public record DataExportJobRecord(
        UUID id,
        UUID privacyRequestId,
        UUID userId,
        UUID studioId,
        ExportType exportType,
        String storageKey,
        Long fileSizeBytes,
        String downloadTokenHash,
        int downloadCount,
        DataExportStatus status,
        Instant expiresAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {}
