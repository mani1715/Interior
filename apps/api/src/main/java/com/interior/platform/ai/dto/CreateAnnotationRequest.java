package com.interior.platform.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAnnotationRequest(
        @NotNull UUID jobId,
        @DecimalMin(value = "0.0", message = "X coordinate must be >= 0.0")
        @DecimalMax(value = "1.0", message = "X coordinate must be <= 1.0")
        double coordX,

        @DecimalMin(value = "0.0", message = "Y coordinate must be >= 0.0")
        @DecimalMax(value = "1.0", message = "Y coordinate must be <= 1.0")
        double coordY,

        @NotBlank @Size(max = 100)
        String authorName,

        @NotBlank @Size(max = 1000)
        String commentText,

        boolean isChangeRequest,
        UUID parentAnnotationId
) {}
