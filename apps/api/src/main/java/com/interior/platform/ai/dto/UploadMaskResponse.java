package com.interior.platform.ai.dto;

import java.util.UUID;

public record UploadMaskResponse(
        UUID maskId,
        String maskStorageKey,
        int width,
        int height,
        double coverageRatio
) {}
