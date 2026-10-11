package com.interior.platform.operations.domain;

import java.time.Instant;
import java.util.UUID;

public record OperationalJobRunRecord(
        UUID id,
        String jobKey,
        String instanceId,
        String status,
        Instant startedAt,
        Instant completedAt,
        int processedCount,
        String failureCode,
        String errorSummary
) {}
