package com.interior.platform.projects.dto;

import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.VisibilityStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProjectRequest(
        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
        String title,

        @NotNull(message = "Category is required")
        ProjectCategory categoryCode,

        @Size(max = 300, message = "Short description must not exceed 300 characters")
        String shortDescription,

        @Size(max = 4000, message = "Full description must not exceed 4000 characters")
        String fullDescription,

        PropertyType propertyType,

        ProjectScope projectScope,

        List<ProjectStyle> styleCodes,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,

        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @Size(max = 2, message = "Country code must be 2 characters")
        String country,

        @Min(value = 1990, message = "Completion year must be 1990 or later")
        @Max(value = 2100, message = "Completion year must be realistic")
        Integer completionYear,

        BudgetVisibility budgetVisibility,

        @Min(value = 0, message = "Budget minimum cannot be negative")
        BigDecimal budgetMin,

        @Min(value = 0, message = "Budget maximum cannot be negative")
        BigDecimal budgetMax,

        String currency,

        ClientNameVisibility clientNameVisibility,

        @Size(max = 100, message = "Client name must not exceed 100 characters")
        String clientDisplayName,

        @Min(value = 0, message = "Area cannot be negative")
        BigDecimal areaValue,

        AreaUnit areaUnit,

        VisibilityStatus visibilityStatus,

        Boolean featured,

        @Size(max = 2000, message = "Internal notes must not exceed 2000 characters")
        String internalNotes
) {}
