package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.AiJobStatus;

import java.time.Instant;
import java.util.UUID;

public record AiJobDetailResponse(
        UUID id,
        UUID studioId,
        UUID projectId,
        UUID inputMediaId,
        String inputPreviewUrl,
        UUID outputMediaId,
        String outputPreviewUrl,
        String providerKey,
        String prompt,
        AiJobStatus status,
        String errorCode,
        String errorMessageSafe,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt
) {}
