package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.EditingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateAiJobRequest(
        @NotNull(message = "inputMediaId is required")
        UUID inputMediaId,

        @NotNull(message = "projectId is required")
        UUID projectId,

        @NotBlank(message = "Prompt cannot be blank")
        @Size(min = 5, max = 500, message = "Prompt must be between 5 and 500 characters")
        @Pattern(regexp = "^[^<>]*$", message = "Prompt must be plain text without HTML tags")
        String prompt,

        @Size(max = 128, message = "Idempotency key must not exceed 128 characters")
        String idempotencyKey,

        Boolean preserveStructure,

        @Size(max = 4, message = "Maximum of 4 reference images can be attached")
        List<@Valid AiJobReferenceInput> references,

        EditingMode editingMode,

        @Size(max = 512, message = "maskStorageKey must not exceed 512 characters")
        String maskStorageKey
) {
    public CreateAiJobRequest(UUID inputMediaId, UUID projectId, String prompt, String idempotencyKey) {
        this(inputMediaId, projectId, prompt, idempotencyKey, true, List.of(), EditingMode.FULL_IMAGE, null);
    }

    public CreateAiJobRequest(UUID inputMediaId, UUID projectId, String prompt, String idempotencyKey, Boolean preserveStructure, List<AiJobReferenceInput> references) {
        this(inputMediaId, projectId, prompt, idempotencyKey, preserveStructure, references, EditingMode.FULL_IMAGE, null);
    }

    public CreateAiJobRequest {
        if (preserveStructure == null) {
            preserveStructure = true;
        }
        if (references == null) {
            references = List.of();
        }
        if (editingMode == null) {
            editingMode = EditingMode.FULL_IMAGE;
        }
    }
}
