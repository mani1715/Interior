package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ModerateReviewRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "PUBLISHED|FLAGGED|REMOVED", message = "Status must be PUBLISHED, FLAGGED, or REMOVED")
        String status,

        String reason
) {}
