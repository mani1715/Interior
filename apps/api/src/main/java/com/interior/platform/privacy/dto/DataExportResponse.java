package com.interior.platform.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record DataExportResponse(
        UUID jobId,
        UUID privacyRequestId,
        String exportType,
        String status,
        Long fileSizeBytes,
        String downloadToken,
        String downloadUrl,
        Instant expiresAt,
        int downloadCount,
        Instant createdAt
) {}
