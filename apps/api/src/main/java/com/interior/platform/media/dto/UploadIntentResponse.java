package com.interior.platform.media.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadIntentResponse(
        UUID uploadIntentId,
        UUID mediaAssetId,
        String uploadUrl,
        String quarantineKey,
        Instant expiresAt
) {}
