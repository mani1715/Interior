package com.interior.platform.media.dto;

import com.interior.platform.media.domain.WatermarkPosition;

import java.math.BigDecimal;
import java.util.UUID;

public record WatermarkSettingsResponse(
        UUID studioId,
        boolean enabled,
        WatermarkPosition position,
        BigDecimal opacity,
        int sizePercentage,
        boolean useLogo,
        String fallbackText
) {}
