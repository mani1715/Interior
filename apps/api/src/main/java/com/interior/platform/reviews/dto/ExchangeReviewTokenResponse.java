package com.interior.platform.reviews.dto;

import java.util.UUID;

public record ExchangeReviewTokenResponse(
        UUID invitationId,
        String csrfToken,
        String submitUrl,
        String studioName,
        String studioSlug,
        String clientFirstName
) {
}
