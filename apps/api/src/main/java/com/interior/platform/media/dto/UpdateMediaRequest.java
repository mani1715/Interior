package com.interior.platform.media.dto;

import com.interior.platform.media.domain.MediaVisibility;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateMediaRequest(
        @Size(max = 255, message = "Alt text cannot exceed 255 characters")
        String altText,

        @Size(max = 1000, message = "Caption cannot exceed 1000 characters")
        String caption,

        Boolean isCover,

        MediaVisibility visibility,

        Boolean watermarkEnabled,

        Integer sortOrder,

        UUID roomId,

        Boolean clearRoom,

        Boolean isRoomCover,

        @DecimalMin(value = "0.0", message = "Focal X must be at least 0")
        @DecimalMax(value = "100.0", message = "Focal X cannot exceed 100")
        BigDecimal focalX,

        @DecimalMin(value = "0.0", message = "Focal Y must be at least 0")
        @DecimalMax(value = "100.0", message = "Focal Y cannot exceed 100")
        BigDecimal focalY,

        Boolean motionEnabled
) {
    public UpdateMediaRequest(
            String altText,
            String caption,
            Boolean isCover,
            MediaVisibility visibility,
            Boolean watermarkEnabled,
            Integer sortOrder
    ) {
        this(altText, caption, isCover, visibility, watermarkEnabled, sortOrder, null, null, null, null, null, null);
    }
}
