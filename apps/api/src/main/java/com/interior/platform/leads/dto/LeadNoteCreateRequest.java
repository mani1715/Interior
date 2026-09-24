package com.interior.platform.leads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadNoteCreateRequest(
    @NotBlank(message = "Note content is required")
    @Size(max = 4000, message = "Note content must not exceed 4000 characters")
    String content
) {}
