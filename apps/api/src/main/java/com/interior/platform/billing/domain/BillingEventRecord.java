package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BillingEventRecord(
        UUID id,
        UUID studioId,
        String eventType,
        String provider,
        String providerEventId,
        Map<String, Object> details,
        Instant occurredAt,
        Instant createdAt
) {}
