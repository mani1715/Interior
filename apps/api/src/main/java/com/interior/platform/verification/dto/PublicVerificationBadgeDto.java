package com.interior.platform.verification.dto;

import java.time.Instant;

public record PublicVerificationBadgeDto(
        boolean isVerified,
        String badgeLabel,
        String description,
        Instant verifiedAt
) {
    public static PublicVerificationBadgeDto verified(Instant verifiedAt) {
        return new PublicVerificationBadgeDto(
                true,
                "Verified Business",
                "Business details were reviewed by the platform. This does not guarantee service quality or project outcomes.",
                verifiedAt
        );
    }

    public static PublicVerificationBadgeDto unverified() {
        return new PublicVerificationBadgeDto(
                false,
                null,
                null,
                null
        );
    }
}
