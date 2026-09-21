package com.interior.platform.seo.dto;

import jakarta.validation.constraints.Size;

public record UpdateSeoSettingsRequest(
        @Size(max = 120, message = "Meta title override cannot exceed 120 characters")
        String metaTitleOverride,

        @Size(max = 300, message = "Meta description override cannot exceed 300 characters")
        String metaDescriptionOverride,

        @Size(max = 500, message = "Canonical URL override cannot exceed 500 characters")
        String canonicalUrlOverride,

        Boolean indexingEnabled
) {}
