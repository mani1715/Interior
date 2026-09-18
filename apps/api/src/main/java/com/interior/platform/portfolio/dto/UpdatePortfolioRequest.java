package com.interior.platform.portfolio.dto;

import com.interior.platform.portfolio.domain.FontPairing;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePortfolioRequest(
        @Size(max = 200, message = "Headline must not exceed 200 characters")
        String headline,

        @Size(max = 300, message = "Subheadline must not exceed 300 characters")
        String subheadline,

        @Size(max = 4000, message = "Bio must not exceed 4000 characters")
        String bio,

        @Size(max = 2000, message = "Design philosophy must not exceed 2000 characters")
        String designPhilosophy,

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 100, message = "Years of experience must be realistic")
        Integer yearsOfExperience,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Primary color must be a valid hex color code (e.g. #2C3E50)")
        String primaryColor,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Secondary color must be a valid hex color code (e.g. #E8DCC4)")
        String secondaryColor,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Accent color must be a valid hex color code (e.g. #D4AF37)")
        String accentColor,

        FontPairing fontPairing,

        String status,

        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
