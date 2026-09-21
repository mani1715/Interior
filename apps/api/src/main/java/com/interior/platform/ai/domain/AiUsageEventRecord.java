package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiUsageEventRecord(
        UUID id,
        UUID studioId,
        UUID jobId,
        String eventType,
        String providerKey,
        int unitsConsumed,
        Instant createdAt
) {}
