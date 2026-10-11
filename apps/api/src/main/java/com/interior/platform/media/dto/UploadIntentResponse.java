package com.interior.platform.media.dto;

import com.interior.platform.media.domain.ScanStatus;

import java.time.Instant;
import java.util.UUID;

public record UploadIntentResponse(
        UUID uploadIntentId,
        UUID mediaAssetId,
        String uploadUrl,
        String quarantineKey,
        Instant expiresAt,
        ScanStatus scanStatus,
        String scannerProvider,
        String threatName
) {
    public UploadIntentResponse(
            UUID uploadIntentId,
            UUID mediaAssetId,
            String uploadUrl,
            String quarantineKey,
            Instant expiresAt
    ) {
        this(
                uploadIntentId,
                mediaAssetId,
                uploadUrl,
                quarantineKey,
                expiresAt,
                ScanStatus.NOT_SCANNED,
                null,
                null
        );
    }
}
