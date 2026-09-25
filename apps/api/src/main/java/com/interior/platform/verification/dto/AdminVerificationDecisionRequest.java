package com.interior.platform.verification.dto;

import com.interior.platform.verification.domain.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminVerificationDecisionRequest(
        @NotNull(message = "Decision status is required")
        VerificationStatus status,

        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason
) {
}
