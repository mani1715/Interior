package com.interior.platform.privacy.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateLegalHoldRequest(
        @NotBlank String subjectType,
        UUID subjectId,
        @NotBlank String reason
) {}
