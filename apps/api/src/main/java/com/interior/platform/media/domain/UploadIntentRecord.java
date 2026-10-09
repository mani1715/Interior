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
        Instant createdAt,
        UUID mediaAssetId
) {
    public UploadIntentRecord(
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
    ) {
        this(id, studioId, projectId, mediaType, expectedContentType, expectedSizeBytes, quarantineKey, status, expiresAt, createdBy, createdAt, null);
    }
}
