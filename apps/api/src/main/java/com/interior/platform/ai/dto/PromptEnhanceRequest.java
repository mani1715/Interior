package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PromptEnhanceRequest(
        @NotBlank(message = "Prompt text is required")
        @Size(max = 1000, message = "Prompt text cannot exceed 1000 characters")
        String prompt,

        String roomType,

        String editingMode,

        Boolean preserveStructure,

        String architecturalStyle,

        List<String> referencePurposes
) {
}
