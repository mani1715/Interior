package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record BillingCheckoutSessionRecord(
        UUID id,
        UUID studioId,
        UUID planId,
        String provider,
        String providerSessionId,
        String idempotencyKey,
        String status,
        long amountMinor,
        String currency,
        String successUrl,
        String cancelUrl,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {}
