package com.interior.platform.media.dto;

import com.interior.platform.media.domain.WatermarkPosition;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WatermarkSettingsRequest(
        @NotNull(message = "Enabled flag is required")
        Boolean enabled,

        @NotNull(message = "Watermark position is required")
        WatermarkPosition position,

        @NotNull(message = "Opacity is required")
        @DecimalMin(value = "0.10", message = "Opacity must be at least 0.10")
        @DecimalMax(value = "1.00", message = "Opacity cannot exceed 1.00")
        BigDecimal opacity,

        @Min(value = 5, message = "Size percentage must be at least 5%")
        @Max(value = 30, message = "Size percentage cannot exceed 30%")
        int sizePercentage,

        Boolean useLogo,

        @Size(max = 100, message = "Fallback text cannot exceed 100 characters")
        String fallbackText
) {}
