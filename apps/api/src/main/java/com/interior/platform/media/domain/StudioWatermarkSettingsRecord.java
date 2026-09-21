package com.interior.platform.media.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StudioWatermarkSettingsRecord(
        UUID studioId,
        boolean enabled,
        WatermarkPosition position,
        BigDecimal opacity,
        int sizePercentage,
        boolean useLogo,
        String fallbackText,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudioWatermarkSettingsRecord defaultForStudio(UUID studioId, String fallbackText) {
        return new StudioWatermarkSettingsRecord(
                studioId,
                true,
                WatermarkPosition.BOTTOM_RIGHT,
                new BigDecimal("0.60"),
                15,
                false,
                fallbackText,
                Instant.now(),
                Instant.now()
        );
    }
}
