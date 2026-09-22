package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ClientReviewDecisionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitClientDecisionRequest(
        @NotNull(message = "Job ID is required")
        UUID jobId,

        @NotNull(message = "Decision is required")
        ClientReviewDecisionType decision,

        @Size(max = 100, message = "Client name cannot exceed 100 characters")
        String clientName,

        @Size(max = 1000, message = "Feedback cannot exceed 1000 characters")
        String feedback
) {}
