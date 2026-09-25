package com.interior.platform.verification.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificationDocumentDto(
        UUID id,
        String documentType,
        String originalFilename,
        String mimeType,
        long fileSizeBytes,
        Instant createdAt
) {
}
