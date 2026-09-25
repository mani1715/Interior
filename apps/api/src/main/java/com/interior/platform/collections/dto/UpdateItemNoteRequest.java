package com.interior.platform.collections.dto;

import jakarta.validation.constraints.Size;

public record UpdateItemNoteRequest(
        @Size(max = 500, message = "Note cannot exceed 500 characters")
        String note
) {
}
