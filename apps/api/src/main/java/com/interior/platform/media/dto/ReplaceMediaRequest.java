package com.interior.platform.media.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReplaceMediaRequest(
        @NotNull(message = "Upload intent ID is required")
        UUID uploadIntentId
) {}
