package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ReferencePurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AiJobReferenceInput(
        @NotNull(message = "mediaId is required")
        UUID mediaId,

        @NotNull(message = "purpose is required")
        ReferencePurpose purpose,

        @Size(max = 100, message = "Label must not exceed 100 characters")
        String label,

        @Size(max = 300, message = "Instruction must not exceed 300 characters")
        String instruction,

        int displayOrder
) {}
