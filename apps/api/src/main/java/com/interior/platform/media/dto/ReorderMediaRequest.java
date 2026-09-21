package com.interior.platform.media.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderMediaRequest(
        @NotEmpty(message = "Ordered media IDs cannot be empty")
        List<UUID> orderedMediaIds
) {}
