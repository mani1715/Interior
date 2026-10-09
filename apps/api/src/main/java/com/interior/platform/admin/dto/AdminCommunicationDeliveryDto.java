package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminCommunicationDeliveryDto(
        UUID id,
        UUID studioId,
        String channel,
        String eventType,
        String maskedRecipient,
        String subjectOrSummary,
        String status,
        String provider,
        int attemptCount,
        int maxAttempts,
        String lastError,
        Instant createdAt
) {}
