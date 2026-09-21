package com.interior.platform.media.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaDerivativeRecord(
        UUID id,
        UUID mediaId,
        UUID studioId,
        DerivativeVariant variantName,
        int width,
        int height,
        String format,
        long fileSize,
        String storageKey,
        String publicUrl,
        boolean isWatermarked,
        Instant createdAt
) {}
