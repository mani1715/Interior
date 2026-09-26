package com.interior.platform.billing.dto;

import java.util.Map;
import java.util.UUID;

public record BillingPlanDto(
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
        Map<String, Object> entitlements
) {}
