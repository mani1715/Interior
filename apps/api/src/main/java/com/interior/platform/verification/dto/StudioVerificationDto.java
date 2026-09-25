package com.interior.platform.verification.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioVerificationDto(
        UUID id,
        UUID studioId,
        String status,
        String businessName,
        String professionalType,
        String registrationNumber,
        String gstNumber,
        String websiteDomain,
        String notes,
        String decisionReason,
        Instant verifiedAt,
        Instant expiresAt,
        List<VerificationDocumentDto> documents,
        List<VerificationEventDto> events
) {
}
