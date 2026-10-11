package com.interior.platform.media.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaSecurityScanRecord(
        UUID id,
        UUID uploadIntentId,
        UUID studioId,
        String storageKey,
        String scannerProvider,
        String engineVersion,
        ScanOutcome scanOutcome,
        String threatClassification,
        long durationMs,
        Instant scannedAt,
        String errorMessage,
        String metadata
) {}
