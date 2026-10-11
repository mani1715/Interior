package com.interior.platform.media.security.scanner;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ScanMetadata(
        UUID uploadIntentId,
        UUID studioId,
        UUID projectId,
        String contentType,
        long sizeBytes,
        String clientFileName,
        Map<String, String> extra
) {
    public ScanMetadata(UUID uploadIntentId, UUID studioId, UUID projectId, String contentType, long sizeBytes) {
        this(uploadIntentId, studioId, projectId, contentType, sizeBytes, null, Map.of());
    }
}
