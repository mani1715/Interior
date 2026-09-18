package com.interior.platform.workspace.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceSummaryResponse(
        WorkspaceUserDto user,
        WorkspaceStudioDto studio,
        List<StudioMembershipSummaryDto> availableStudios,
        CompletenessDto completeness,
        List<SetupChecklistItemDto> setupChecklist,
        List<ModuleReadinessDto> modules,
        List<ActivityItemDto> activityFeed
) {
    public record WorkspaceUserDto(
            String displayName,
            String email,
            List<String> roles
    ) {}

    public record WorkspaceStudioDto(
            UUID id,
            String name,
            String slug,
            String professionalType,
            String professionalTitle,
            String tagline,
            String role,
            String operationalStatus,
            String publicationStatus,
            String city,
            String state,
            int serviceCount,
            int specialtyCount,
            int serviceAreaCount,
            int contactCount
    ) {}

    public record StudioMembershipSummaryDto(
            UUID studioId,
            String studioName,
            String studioSlug,
            String role
    ) {}

    public record CompletenessDto(
            int profileCompletenessPercentage,
            int platformReadinessPercentage,
            String status,
            CompletenessBreakdownDto breakdown
    ) {}

    public record CompletenessBreakdownDto(
            int identityScore,
            int locationScore,
            int servicesScore,
            int specialtiesScore,
            int contactsScore,
            int portfolioScore,
            int projectsScore
    ) {}

    public record SetupChecklistItemDto(
            String id,
            String title,
            String description,
            boolean completed,
            String actionLabel,
            String actionRoute
    ) {}

    public record ModuleReadinessDto(
            String id,
            String name,
            String description,
            String status, // "READY", "NOT_CONFIGURED", "NOT_STARTED", "COMING_SOON", "LOCKED"
            String route,
            String ctaLabel
    ) {}

    public record ActivityItemDto(
            String id,
            String title,
            String description,
            Instant timestamp,
            String type
    ) {}
}
