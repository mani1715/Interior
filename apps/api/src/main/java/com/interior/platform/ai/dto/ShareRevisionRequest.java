package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ShareRevisionRequest(
        @NotEmpty List<UUID> conceptJobIds,
        String updateMessage
) {}
