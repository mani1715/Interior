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
        UUID mediaAssetId,
        ScanStatus scanStatus,
        int scanAttempt,
        Instant scanRequestedAt,
        Instant scannedAt,
        String scannerProvider,
        String threatName
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
            Instant createdAt,
            UUID mediaAssetId
    ) {
        this(
                id,
                studioId,
                projectId,
                mediaType,
                expectedContentType,
                expectedSizeBytes,
                quarantineKey,
                status,
                expiresAt,
                createdBy,
                createdAt,
                mediaAssetId,
                ScanStatus.NOT_SCANNED,
                0,
                null,
                null,
                null,
                null
        );
    }

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
        this(
                id,
                studioId,
                projectId,
                mediaType,
                expectedContentType,
                expectedSizeBytes,
                quarantineKey,
                status,
                expiresAt,
                createdBy,
                createdAt,
                null,
                ScanStatus.NOT_SCANNED,
                0,
                null,
                null,
                null,
                null
        );
    }
}
