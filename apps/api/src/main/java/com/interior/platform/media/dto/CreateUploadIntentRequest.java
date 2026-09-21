package com.interior.platform.media.dto;

import com.interior.platform.media.domain.MediaType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateUploadIntentRequest(
        @NotNull(message = "Project ID is required")
        UUID projectId,

        @NotNull(message = "Media type is required")
        MediaType mediaType,

        @NotBlank(message = "Content type is required")
        @Pattern(regexp = "^image/(jpeg|png|webp)$", message = "Content type must be image/jpeg, image/png, or image/webp")
        String expectedContentType,

        @Min(value = 1, message = "File size must be greater than 0")
        @Max(value = 26214400, message = "File size cannot exceed 25MB")
        long expectedSizeBytes,

        String clientFileName
) {}
