package com.interior.platform.media.dto;

import com.interior.platform.media.domain.MediaVisibility;
import jakarta.validation.constraints.Size;

public record UpdateMediaRequest(
        @Size(max = 255, message = "Alt text cannot exceed 255 characters")
        String altText,

        @Size(max = 1000, message = "Caption cannot exceed 1000 characters")
        String caption,

        Boolean isCover,

        MediaVisibility visibility,

        Boolean watermarkEnabled,

        Integer sortOrder
) {}
