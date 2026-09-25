package com.interior.platform.verification.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioVerificationRecord(
        UUID id,
        UUID studioId,
        VerificationStatus status,
        String businessName,
        String professionalType,
        String registrationNumber,
        String gstNumber,
        String websiteDomain,
        String notes,
        String decisionReason,
        Instant verifiedAt,
        Instant expiresAt,
        String verifiedSnapshot,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public boolean isVerified() {
        if (status != VerificationStatus.VERIFIED) {
            return false;
        }
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
}
