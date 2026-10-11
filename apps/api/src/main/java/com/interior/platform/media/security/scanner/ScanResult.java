package com.interior.platform.media.security.scanner;

import com.interior.platform.media.domain.ScanOutcome;

import java.time.Instant;
import java.util.Map;

public record ScanResult(
        ScanOutcome outcome,
        String provider,
        String engineVersion,
        String threatClassification,
        long durationMs,
        Instant scannedAt,
        String errorMessage,
        Map<String, String> details
) {
    public static ScanResult clean(String provider, String engineVersion, long durationMs) {
        return new ScanResult(
                ScanOutcome.CLEAN,
                provider,
                engineVersion,
                null,
                durationMs,
                Instant.now(),
                null,
                Map.of()
        );
    }

    public static ScanResult infected(String provider, String engineVersion, String threatClassification, long durationMs) {
        return new ScanResult(
                ScanOutcome.INFECTED,
                provider,
                engineVersion,
                threatClassification,
                durationMs,
                Instant.now(),
                null,
                Map.of()
        );
    }

    public static ScanResult error(String provider, String errorMessage, long durationMs) {
        return new ScanResult(
                ScanOutcome.ERROR,
                provider,
                null,
                null,
                durationMs,
                Instant.now(),
                errorMessage,
                Map.of()
        );
    }

    public static ScanResult unavailable(String provider, String reason) {
        return new ScanResult(
                ScanOutcome.UNAVAILABLE,
                provider,
                null,
                null,
                0,
                Instant.now(),
                reason,
                Map.of()
        );
    }
}
