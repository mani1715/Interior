package com.interior.platform.email.domain;

import java.time.Instant;
import java.util.UUID;

public record CommunicationDeliveryRecord(
        UUID id,
        UUID studioId,
        UUID recipientUserId,
        DeliveryChannel channel,
        String eventType,
        String recipient,
        String subjectOrSummary,
        DeliveryStatus status,
        String provider,
        String providerMessageId,
        int attemptCount,
        int maxAttempts,
        String lastError,
        Instant nextRetryAt,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt
) {}
