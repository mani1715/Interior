package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.ai.domain.VariationStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateVariationRequest(
        VariationStrategy variationStrategy,

        @Size(max = 2000, message = "Prompt cannot exceed 2000 characters")
        String prompt,

        Boolean preserveStructure,

        EditingMode editingMode,

        Boolean reuseParentMask,

        UUID newMaskSourceJobId,

        List<@Valid AiJobReferenceInput> references,

        String idempotencyKey
) {
    public VariationStrategy resolvedStrategy() {
        return variationStrategy != null ? variationStrategy : VariationStrategy.REFINE_ORIGINAL;
    }
}
