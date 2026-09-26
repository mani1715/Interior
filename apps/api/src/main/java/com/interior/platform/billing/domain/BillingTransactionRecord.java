package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record BillingTransactionRecord(
        UUID id,
        UUID studioId,
        UUID subscriptionId,
        String provider,
        String providerPaymentId,
        String providerOrderId,
        long amountMinor,
        String currency,
        BillingTransactionStatus status,
        String description,
        String receiptUrl,
        Instant occurredAt,
        Instant createdAt
) {}
