package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiJobRecord(
        UUID id,
        UUID studioId,
        UUID projectId,
        UUID inputMediaId,
        UUID outputMediaId,
        String providerKey,
        String providerJobId,
        String prompt,
        String systemPrompt,
        AiJobStatus status,
        String errorCode,
        String errorMessageSafe,
        int attemptCount,
        String idempotencyKey,
        UUID createdBy,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String usageMetadata,
        long version
) {}
