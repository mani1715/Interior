package com.interior.platform.media.dto;

import com.interior.platform.media.domain.DerivativeVariant;

import java.util.UUID;

public record MediaDerivativeDto(
        UUID id,
        DerivativeVariant variantName,
        int width,
        int height,
        String format,
        long fileSize,
        String publicUrl,
        boolean isWatermarked
) {}
