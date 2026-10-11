package com.interior.platform.support.domain;

import java.time.Instant;
import java.util.UUID;

public record SupportRequestRecord(
        UUID id,
        UUID userId,
        UUID studioId,
        SupportCategory category,
        String subject,
        SupportStatus status,
        SupportPriority priority,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {}
