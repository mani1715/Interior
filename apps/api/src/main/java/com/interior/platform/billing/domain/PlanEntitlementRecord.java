package com.interior.platform.billing.domain;

import java.time.Instant;
import java.util.UUID;

public record PlanEntitlementRecord(
        UUID id,
        UUID planId,
        String entitlementKey,
        String valueType,
        Boolean booleanValue,
        Long numericValue,
        Instant createdAt
) {}
