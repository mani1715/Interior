package com.interior.platform.projects.repository;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProjectRepository implements ProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudioProjectRecord> projectMapper = (rs, rowNum) -> new StudioProjectRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            rs.getString("slug"),
            rs.getString("title"),
            rs.getString("short_description"),
            rs.getString("full_description"),
            ProjectCategory.valueOf(rs.getString("category_code")),
            ProjectStatus.valueOf(rs.getString("project_status")),
            VisibilityStatus.valueOf(rs.getString("visibility_status")),
            rs.getBoolean("featured"),
            rs.getInt("display_order"),
            rs.getString("city"),
            rs.getString("district"),
            rs.getString("state"),
            rs.getString("country"),
            rs.getString("property_type") != null ? PropertyType.valueOf(rs.getString("property_type")) : null,
            rs.getString("project_scope") != null ? ProjectScope.valueOf(rs.getString("project_scope")) : null,
            rs.getObject("completion_year") != null ? rs.getInt("completion_year") : null,
            BudgetVisibility.valueOf(rs.getString("budget_visibility")),
            rs.getBigDecimal("budget_min"),
            rs.getBigDecimal("budget_max"),
            rs.getString("currency"),
            ClientNameVisibility.valueOf(rs.getString("client_name_visibility")),
            rs.getString("client_display_name"),
            rs.getBigDecimal("area_value"),
            rs.getString("area_unit") != null ? AreaUnit.valueOf(rs.getString("area_unit")) : null,
            rs.getString("internal_notes"),
            rs.getLong("version"),
            getUuid(rs, "created_by"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toInstant() : null
    );

    @Override
    public StudioProjectRecord createProject(StudioProjectRecord project, List<ProjectStyle> styles) {
        String sql = "INSERT INTO studio_projects (" +
                     "id, studio_id, slug, title, short_description, full_description, category_code, " +
                     "project_status, visibility_status, featured, display_order, city, district, state, country, " +
                     "property_type, project_scope, completion_year, budget_visibility, budget_min, budget_max, " +
                     "currency, client_name_visibility, client_display_name, area_value, area_unit, internal_notes, " +
                     "version, created_by, created_at, updated_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                project.id(),
                project.studioId(),
                project.slug(),
                project.title(),
                project.shortDescription(),
                project.fullDescription(),
                project.categoryCode().name(),
                project.projectStatus().name(),
                project.visibilityStatus().name(),
                project.featured(),
                project.displayOrder(),
                project.city(),
                project.district(),
                project.state(),
                project.country(),
                project.propertyType() != null ? project.propertyType().name() : null,
                project.projectScope() != null ? project.projectScope().name() : null,
                project.completionYear(),
                project.budgetVisibility().name(),
                project.budgetMin(),
                project.budgetMax(),
                project.currency(),
                project.clientNameVisibility().name(),
                project.clientDisplayName(),
                project.areaValue(),
                project.areaUnit() != null ? project.areaUnit().name() : null,
                project.internalNotes(),
                project.version(),
                project.createdBy(),
                Timestamp.from(project.createdAt()),
                Timestamp.from(project.updatedAt())
        );

        saveStyles(project.id(), project.studioId(), styles);
        return project;
    }

    @Override
    public int updateProject(StudioProjectRecord project, List<ProjectStyle> styles, long expectedVersion) {
        String sql = "UPDATE studio_projects SET " +
                     "slug = ?, " +
                     "title = ?, " +
                     "short_description = ?, " +
                     "full_description = ?, " +
                     "category_code = ?, " +
                     "project_status = ?, " +
                     "visibility_status = ?, " +
                     "featured = ?, " +
                     "city = ?, " +
                     "district = ?, " +
                     "state = ?, " +
                     "country = ?, " +
                     "property_type = ?, " +
                     "project_scope = ?, " +
                     "completion_year = ?, " +
                     "budget_visibility = ?, " +
                     "budget_min = ?, " +
                     "budget_max = ?, " +
                     "currency = ?, " +
                     "client_name_visibility = ?, " +
                     "client_display_name = ?, " +
                     "area_value = ?, " +
                     "area_unit = ?, " +
                     "internal_notes = ?, " +
                     "version = version + 1, " +
                     "updated_at = now() " +
                     "WHERE id = ? AND studio_id = ? AND version = ?";

        int updated = jdbcTemplate.update(sql,
                project.slug(),
                project.title(),
                project.shortDescription(),
                project.fullDescription(),
                project.categoryCode().name(),
                project.projectStatus().name(),
                project.visibilityStatus().name(),
                project.featured(),
                project.city(),
                project.district(),
                project.state(),
                project.country(),
                project.propertyType() != null ? project.propertyType().name() : null,
                project.projectScope() != null ? project.projectScope().name() : null,
                project.completionYear(),
                project.budgetVisibility().name(),
                project.budgetMin(),
                project.budgetMax(),
                project.currency(),
                project.clientNameVisibility().name(),
                project.clientDisplayName(),
                project.areaValue(),
                project.areaUnit() != null ? project.areaUnit().name() : null,
                project.internalNotes(),
                project.id(),
                project.studioId(),
                expectedVersion
        );

        if (updated > 0) {
            saveStyles(project.id(), project.studioId(), styles);
        }
        return updated;
    }

    private void saveStyles(UUID projectId, UUID studioId, List<ProjectStyle> styles) {
        jdbcTemplate.update("DELETE FROM project_styles WHERE project_id = ? AND studio_id = ?", projectId, studioId);
        if (styles != null && !styles.isEmpty()) {
            String insertSql = "INSERT INTO project_styles (id, project_id, studio_id, style_code, created_at) " +
                               "VALUES (?, ?, ?, ?, now())";
            for (ProjectStyle style : styles) {
                jdbcTemplate.update(insertSql, UuidV7.randomUuid(), projectId, studioId, style.name());
            }
        }
    }

    @Override
    public Optional<StudioProjectRecord> findProjectById(UUID studioId, UUID projectId) {
        String sql = "SELECT * FROM studio_projects WHERE studio_id = ? AND id = ?";
        List<StudioProjectRecord> results = jdbcTemplate.query(sql, projectMapper, studioId, projectId);
        return results.stream().findFirst();
    }

    @Override
    public Optional<StudioProjectRecord> findProjectBySlug(UUID studioId, String slug) {
        String sql = "SELECT * FROM studio_projects WHERE studio_id = ? AND slug = ?";
        List<StudioProjectRecord> results = jdbcTemplate.query(sql, projectMapper, studioId, slug);
        return results.stream().findFirst();
    }

    @Override
    public List<ProjectStyle> findStylesByProjectId(UUID projectId) {
        String sql = "SELECT style_code FROM project_styles WHERE project_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> ProjectStyle.valueOf(rs.getString("style_code")), projectId);
    }

    @Override
    public Map<UUID, List<ProjectStyle>> findStylesByProjectIds(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String inSql = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT project_id, style_code FROM project_styles WHERE project_id IN (" + inSql + ") ORDER BY created_at ASC";
        Map<UUID, List<ProjectStyle>> map = new HashMap<>();
        for (UUID pid : projectIds) {
            map.put(pid, new ArrayList<>());
        }
        jdbcTemplate.query(sql, rs -> {
            UUID pid = getUuid(rs, "project_id");
            ProjectStyle style = ProjectStyle.valueOf(rs.getString("style_code"));
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(style);
        }, projectIds.toArray());
        return map;
    }

    @Override
    public List<StudioProjectRecord> listProjects(
            UUID studioId,
            ProjectStatus status,
            ProjectCategory category,
            VisibilityStatus visibility,
            Boolean featured,
            boolean includeArchived
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM studio_projects WHERE studio_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(studioId);

        if (!includeArchived) {
            sql.append(" AND project_status != 'ARCHIVED'");
        }
        if (status != null) {
            sql.append(" AND project_status = ?");
            params.add(status.name());
        }
        if (category != null) {
            sql.append(" AND category_code = ?");
            params.add(category.name());
        }
        if (visibility != null) {
            sql.append(" AND visibility_status = ?");
            params.add(visibility.name());
        }
        if (featured != null) {
            sql.append(" AND featured = ?");
            params.add(featured);
        }

        sql.append(" ORDER BY display_order ASC, created_at DESC");
        return jdbcTemplate.query(sql.toString(), projectMapper, params.toArray());
    }

    @Override
    public List<StudioProjectRecord> findPortfolioProjects(UUID studioId) {
        String sql = "SELECT * FROM studio_projects " +
                     "WHERE studio_id = ? AND project_status != 'ARCHIVED' AND visibility_status = 'PORTFOLIO' " +
                     "ORDER BY display_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, projectMapper, studioId);
    }

    @Override
    public int countProjects(UUID studioId) {
        String sql = "SELECT COUNT(*) FROM studio_projects WHERE studio_id = ? AND project_status != 'ARCHIVED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public int countReadyProjects(UUID studioId) {
        String sql = "SELECT COUNT(*) FROM studio_projects WHERE studio_id = ? AND project_status = 'READY'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return count != null ? count : 0;
    }

    @Override
    public void updateDisplayOrders(UUID studioId, List<UUID> projectIdsInOrder) {
        if (projectIdsInOrder == null || projectIdsInOrder.isEmpty()) {
            return;
        }
        String sql = "UPDATE studio_projects SET display_order = ?, updated_at = now() WHERE id = ? AND studio_id = ?";
        for (int i = 0; i < projectIdsInOrder.size(); i++) {
            jdbcTemplate.update(sql, i, projectIdsInOrder.get(i), studioId);
        }
    }

    @Override
    public int archiveProject(UUID studioId, UUID projectId, long expectedVersion) {
        String sql = "UPDATE studio_projects SET " +
                     "project_status = 'ARCHIVED', " +
                     "visibility_status = 'PRIVATE', " +
                     "featured = false, " +
                     "version = version + 1, " +
                     "updated_at = now(), " +
                     "archived_at = now() " +
                     "WHERE id = ? AND studio_id = ? AND version = ?";
        return jdbcTemplate.update(sql, projectId, studioId, expectedVersion);
    }

    @Override
    public int restoreProject(UUID studioId, UUID projectId, long expectedVersion) {
        String sql = "UPDATE studio_projects SET " +
                     "project_status = 'DRAFT', " +
                     "version = version + 1, " +
                     "updated_at = now(), " +
                     "archived_at = null " +
                     "WHERE id = ? AND studio_id = ? AND version = ?";
        return jdbcTemplate.update(sql, projectId, studioId, expectedVersion);
    }

    @Override
    public boolean existsBySlug(UUID studioId, String slug, UUID excludeProjectId) {
        if (excludeProjectId != null) {
            String sql = "SELECT COUNT(*) FROM studio_projects WHERE studio_id = ? AND slug = ? AND id != ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId, slug, excludeProjectId);
            return count != null && count > 0;
        } else {
            String sql = "SELECT COUNT(*) FROM studio_projects WHERE studio_id = ? AND slug = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId, slug);
            return count != null && count > 0;
        }
    }

    @Override
    public int getNextDisplayOrder(UUID studioId) {
        String sql = "SELECT COALESCE(MAX(display_order), -1) + 1 FROM studio_projects WHERE studio_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, studioId);
        return next != null ? next : 0;
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }
}
