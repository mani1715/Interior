package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ReferencePurpose;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateReferenceRequest(
        ReferencePurpose purpose,

        @Size(max = 100, message = "Label must not exceed 100 characters")
        String label,

        @Size(max = 300, message = "Default instruction must not exceed 300 characters")
        String defaultInstruction,

        UUID projectId
) {}
