package com.interior.platform.analytics.domain;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsEventRecord(
        UUID id,
        UUID studioId,
        AnalyticsEventType eventType,
        String entityType,
        UUID entityId,
        String source,
        Instant occurredAt,
        String anonymousSessionHash,
        String metadataJson,
        String deduplicationKey,
        Instant createdAt
) {}
