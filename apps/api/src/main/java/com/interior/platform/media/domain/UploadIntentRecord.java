package com.interior.platform.media.domain;

import java.time.Instant;
import java.util.UUID;

public record UploadIntentRecord(
        UUID id,
        UUID studioId,
        UUID projectId,
        MediaType mediaType,
        String expectedContentType,
        long expectedSizeBytes,
        String quarantineKey,
        UploadIntentStatus status,
        Instant expiresAt,
        UUID createdBy,
        Instant createdAt
) {}
