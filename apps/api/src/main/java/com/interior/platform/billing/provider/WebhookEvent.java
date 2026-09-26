package com.interior.platform.billing.provider;

import java.time.Instant;
import java.util.Map;

public record WebhookEvent(
        String eventId,
        String eventType,
        String providerSubscriptionId,
        String providerPaymentId,
        Long amountMinor,
        String status,
        Instant timestamp,
        Map<String, Object> rawData
) {}
