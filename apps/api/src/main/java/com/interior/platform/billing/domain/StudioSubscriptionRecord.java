package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioSubscriptionRecord(
        UUID id,
        UUID studioId,
        UUID planId,
        SubscriptionStatus status,
        String provider,
        String providerCustomerId,
        String providerSubscriptionId,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {}
