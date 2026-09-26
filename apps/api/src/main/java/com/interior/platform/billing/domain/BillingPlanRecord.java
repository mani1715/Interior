package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record BillingPlanRecord(
        UUID id,
        String code,
        String name,
        String description,
        String billingPeriod,
        String currency,
        long priceMinor,
        boolean active,
        boolean purchasable,
        int displayOrder,
        String providerPriceId,
        Instant createdAt,
        Instant updatedAt
) {}
