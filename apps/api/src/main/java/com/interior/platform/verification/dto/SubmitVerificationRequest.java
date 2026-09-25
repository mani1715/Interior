package com.interior.platform.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitVerificationRequest(
        @NotBlank(message = "Business name is required")
        @Size(max = 150, message = "Business name cannot exceed 150 characters")
        String businessName,

        @NotBlank(message = "Professional type is required")
        @Size(max = 100, message = "Professional type cannot exceed 100 characters")
        String professionalType,

        @Size(max = 100, message = "Registration number cannot exceed 100 characters")
        String registrationNumber,

        @Size(max = 50, message = "GST number cannot exceed 50 characters")
        String gstNumber,

        @Size(max = 255, message = "Website domain cannot exceed 255 characters")
        String websiteDomain,

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        String notes
) {
}
