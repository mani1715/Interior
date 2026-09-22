package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.AiJobStatus;

import java.time.Instant;
import java.util.List;
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
        Instant failedAt,
        boolean preserveStructure,
        List<AiJobReferenceResponse> references
) {
    public AiJobDetailResponse(
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
    ) {
        this(
                id,
                studioId,
                projectId,
                inputMediaId,
                inputPreviewUrl,
                outputMediaId,
                outputPreviewUrl,
                providerKey,
                prompt,
                status,
                errorCode,
                errorMessageSafe,
                createdAt,
                startedAt,
                completedAt,
                failedAt,
                true,
                List.of()
        );
    }
}
