package com.interior.platform.reviews.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReviewInvitationRequest(
        @NotNull(message = "Lead ID is required")
        UUID leadId,
        UUID projectId
) {
}
