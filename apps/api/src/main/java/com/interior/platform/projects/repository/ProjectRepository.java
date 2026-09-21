package com.interior.platform.projects.repository;

import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    StudioProjectRecord createProject(StudioProjectRecord project, List<ProjectStyle> styles);

    int updateProject(StudioProjectRecord project, List<ProjectStyle> styles, long expectedVersion);

    Optional<StudioProjectRecord> findProjectById(UUID studioId, UUID projectId);

    Optional<StudioProjectRecord> findProjectBySlug(UUID studioId, String slug);

    List<ProjectStyle> findStylesByProjectId(UUID projectId);

    Map<UUID, List<ProjectStyle>> findStylesByProjectIds(List<UUID> projectIds);

    List<StudioProjectRecord> listProjects(
            UUID studioId,
            ProjectStatus status,
            ProjectCategory category,
            VisibilityStatus visibility,
            Boolean featured,
            boolean includeArchived
    );

    List<StudioProjectRecord> findPortfolioProjects(UUID studioId);

    int countProjects(UUID studioId);

    int countReadyProjects(UUID studioId);

    void updateDisplayOrders(UUID studioId, List<UUID> projectIdsInOrder);

    int archiveProject(UUID studioId, UUID projectId, long expectedVersion);

    int restoreProject(UUID studioId, UUID projectId, long expectedVersion);

    boolean existsBySlug(UUID studioId, String slug, UUID excludeProjectId);

    int getNextDisplayOrder(UUID studioId);
}
