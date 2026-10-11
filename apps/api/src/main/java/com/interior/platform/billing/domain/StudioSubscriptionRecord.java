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
        long version,
        String sourceType,
        UUID downgradePlanId,
        Instant gracePeriodEnd
) {
    public StudioSubscriptionRecord(
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
    ) {
        this(
                id,
                studioId,
                planId,
                status,
                provider,
                providerCustomerId,
                providerSubscriptionId,
                currentPeriodStart,
                currentPeriodEnd,
                cancelAtPeriodEnd,
                cancelledAt,
                createdAt,
                updatedAt,
                version,
                "PAID_PROVIDER",
                null,
                null
        );
    }
}
