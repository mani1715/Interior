package com.interior.platform.verification.domain;

import java.time.Instant;
import java.util.UUID;

public record VerificationDocumentRecord(
        UUID id,
        UUID verificationId,
        UUID studioId,
        VerificationDocumentType documentType,
        String storageKey,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        Instant createdAt
) {
}
