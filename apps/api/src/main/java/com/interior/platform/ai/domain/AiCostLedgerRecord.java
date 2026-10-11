package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiCostLedgerRecord(
        UUID id,
        UUID studioId,
        UUID jobId,
        String providerKey,
        String modelName,
        String operation,
        int inputUnits,
        int outputUnits,
        long amountMinor,
        String currency,
        CostSource costSource,
        String pricingVersion,
        Instant createdAt
) {}
