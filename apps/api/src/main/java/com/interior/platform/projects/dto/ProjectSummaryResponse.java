package com.interior.platform.projects.dto;

import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.VisibilityStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID id,
        String slug,
        String title,
        ProjectCategory categoryCode,
        String categoryDisplayName,
        ProjectStatus projectStatus,
        VisibilityStatus visibilityStatus,
        boolean featured,
        int displayOrder,
        String city,
        String state,
        PropertyType propertyType,
        ProjectScope projectScope,
        List<ProjectStyle> styleCodes,
        List<String> styleDisplayNames,
        Integer completionYear,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}
