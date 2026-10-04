package com.interior.platform.ai.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetPreferredJobRequest(
        @NotNull UUID jobId
) {}
