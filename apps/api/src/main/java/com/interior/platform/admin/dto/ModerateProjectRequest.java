package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ModerateProjectRequest(
        @NotBlank(message = "Moderation status is required")
        @Pattern(regexp = "APPROVED|FLAGGED|HIDDEN", message = "Status must be APPROVED, FLAGGED, or HIDDEN")
        String moderationStatus,

        String reason
) {}
