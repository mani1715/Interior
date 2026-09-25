package com.interior.platform.discovery.repository;

import com.interior.platform.designers.domain.ProfessionalType;
import com.interior.platform.discovery.dto.*;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStyle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class JdbcDiscoveryRepository implements DiscoveryRepository {

    private final JdbcTemplate jdbcTemplate;
    private Boolean isPostgresCache = null;

    public JdbcDiscoveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private boolean isPostgreSQL() {
        if (isPostgresCache != null) return isPostgresCache;
        try {
            String dbProduct = jdbcTemplate.execute((java.sql.Connection conn) -> conn.getMetaData().getDatabaseProductName());
            isPostgresCache = dbProduct != null && dbProduct.toLowerCase().contains("postgres");
        } catch (Exception e) {
            isPostgresCache = false;
        }
        return isPostgresCache;
    }

    private static final String BASE_PUBLIC_STUDIO_GATE =
            "s.publication_status = 'PUBLISHED' " +
            "AND s.status = 'ACTIVE' ";

    private static final String BASE_PUBLIC_PROJECT_GATE =
            BASE_PUBLIC_STUDIO_GATE +
            "AND p.project_status = 'READY' " +
            "AND p.visibility_status = 'PORTFOLIO' " +
            "AND p.archived_at IS NULL ";

    @Override
    public List<DiscoveryProjectCardDto> searchProjects(DiscoverySearchParams params) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        boolean hasQuery = params.q() != null && !params.q().trim().isEmpty();
        String rawQuery = hasQuery ? params.q().trim() : null;
        String queryPattern = hasQuery ? "%" + rawQuery.toLowerCase() + "%" : null;
        String queryExact = hasQuery ? rawQuery.toLowerCase() : null;

        sql.append("SELECT p.id, p.slug, p.title, p.short_description, p.category_code, ")
           .append("p.city, p.state, p.property_type, p.project_scope, p.completion_year, p.featured, p.created_at, ")
           .append("s.id as studio_id, s.slug as studio_slug, s.name as studio_name, s.professional_type ");

        if (hasQuery) {
            if (isPostgreSQL()) {
                sql.append(", (CASE ")
                   .append("    WHEN LOWER(p.title) = ? THEN 100.0 ")
                   .append("    WHEN LOWER(p.title) LIKE ? THEN 80.0 ")
                   .append("    ELSE (ts_rank(to_tsvector('simple', p.title || ' ' || coalesce(p.short_description, '') || ' ' || coalesce(p.city, '')), plainto_tsquery('simple', ?)) * 40.0 + similarity(p.title, ?) * 40.0) ")
                   .append("END) as search_score ");
                args.add(queryExact);
                args.add(queryExact + "%");
                args.add(rawQuery);
                args.add(rawQuery);
            } else {
                sql.append(", (CASE ")
                   .append("    WHEN LOWER(p.title) = ? THEN 100 ")
                   .append("    WHEN LOWER(p.title) LIKE ? THEN 75 ")
                   .append("    WHEN LOWER(p.title) LIKE ? THEN 50 ")
                   .append("    WHEN LOWER(s.name) LIKE ? THEN 30 ")
                   .append("    WHEN LOWER(p.city) LIKE ? THEN 20 ")
                   .append("    ELSE 10 ")
                   .append("END) as search_score ");

                args.add(queryExact);
                args.add(queryExact + "%");
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
            }
        }

        sql.append("FROM studio_projects p ")
           .append("JOIN designer_studios s ON s.id = p.studio_id ")
           .append("WHERE ").append(BASE_PUBLIC_PROJECT_GATE);

        applyProjectFilters(sql, args, params, rawQuery, queryPattern);

        // Sorting
        String sort = params.sort() != null ? params.sort().trim().toLowerCase() : (hasQuery ? "relevance" : "recent");
        if ("relevance".equals(sort) && hasQuery) {
            sql.append("ORDER BY search_score DESC, p.featured DESC, p.created_at DESC, p.id DESC ");
        } else if ("featured".equals(sort)) {
            sql.append("ORDER BY p.featured DESC, p.created_at DESC, p.id DESC ");
        } else {
            sql.append("ORDER BY p.created_at DESC, p.id DESC ");
        }

        sql.append("LIMIT ? OFFSET ?");
        args.add(params.getSafeLimit());
        args.add(params.getSafeOffset());

        List<ProjectRow> rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ProjectRow(
                getUuid(rs, "id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("short_description"),
                rs.getString("category_code"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("property_type"),
                rs.getString("project_scope"),
                rs.getObject("completion_year") != null ? rs.getInt("completion_year") : null,
                getUuid(rs, "studio_id"),
                rs.getString("studio_slug"),
                rs.getString("studio_name"),
                rs.getString("professional_type")
        ), args.toArray());

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> projectIds = rows.stream().map(ProjectRow::id).toList();

        // 1. Batch fetch styles
        Map<UUID, List<ProjectStyle>> stylesByProject = batchFetchStyles(projectIds);

        // 2. Batch fetch cover images
        Map<UUID, ProjectCoverInfo> coversByProject = batchFetchProjectCovers(projectIds);

        // 3. Assemble DTOs
        List<DiscoveryProjectCardDto> results = new ArrayList<>(rows.size());
        for (ProjectRow r : rows) {
            List<ProjectStyle> styles = stylesByProject.getOrDefault(r.id(), Collections.emptyList());
            List<String> styleCodes = styles.stream().map(ProjectStyle::name).toList();
            List<String> styleNames = styles.stream().map(ProjectStyle::getDisplayName).toList();

            String categoryName = r.categoryCode();
            try {
                categoryName = ProjectCategory.valueOf(r.categoryCode()).getDisplayName();
            } catch (Exception ignored) {}

            String profTypeLabel = r.professionalType();
            try {
                profTypeLabel = ProfessionalType.fromCode(r.professionalType()).getDisplayName();
            } catch (Exception ignored) {}

            ProjectCoverInfo cover = coversByProject.get(r.id());
            String coverUrl = cover != null ? cover.url() : null;
            boolean isAiConcept = cover != null && cover.isAiConcept();

            results.add(new DiscoveryProjectCardDto(
                    r.id(),
                    r.slug(),
                    r.title(),
                    r.shortDescription(),
                    r.categoryCode(),
                    categoryName,
                    styleCodes,
                    styleNames,
                    r.city(),
                    r.state(),
                    r.propertyType(),
                    r.projectScope(),
                    coverUrl,
                    isAiConcept,
                    r.studioId(),
                    r.studioSlug(),
                    r.studioName(),
                    r.professionalType(),
                    profTypeLabel,
                    r.completionYear()
            ));
        }

        return results;
    }

    @Override
    public long countProjects(DiscoverySearchParams params) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        boolean hasQuery = params.q() != null && !params.q().trim().isEmpty();
        String rawQuery = hasQuery ? params.q().trim() : null;
        String queryPattern = hasQuery ? "%" + rawQuery.toLowerCase() + "%" : null;

        sql.append("SELECT COUNT(*) ")
           .append("FROM studio_projects p ")
           .append("JOIN designer_studios s ON s.id = p.studio_id ")
           .append("WHERE ").append(BASE_PUBLIC_PROJECT_GATE);

        applyProjectFilters(sql, args, params, rawQuery, queryPattern);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count != null ? count : 0L;
    }

    private void applyProjectFilters(StringBuilder sql, List<Object> args, DiscoverySearchParams params, String rawQuery, String queryPattern) {
        if (queryPattern != null) {
            if (isPostgreSQL()) {
                sql.append("AND (")
                   .append("to_tsvector('simple', p.title || ' ' || coalesce(p.short_description, '') || ' ' || coalesce(p.city, '')) @@ plainto_tsquery('simple', ?) ")
                   .append("OR LOWER(p.title) LIKE ? ")
                   .append("OR p.title % ? ")
                   .append("OR similarity(p.title, ?) > 0.25 ")
                   .append("OR similarity(s.name, ?) > 0.25 ")
                   .append("OR LOWER(s.name) LIKE ? ")
                   .append("OR LOWER(p.city) LIKE ? ")
                   .append("OR LOWER(p.category_code) LIKE ? ")
                   .append(") ");
                args.add(rawQuery);
                args.add(queryPattern);
                args.add(rawQuery);
                args.add(rawQuery);
                args.add(rawQuery);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
            } else {
                sql.append("AND (")
                   .append("LOWER(p.title) LIKE ? ")
                   .append("OR LOWER(p.short_description) LIKE ? ")
                   .append("OR LOWER(s.name) LIKE ? ")
                   .append("OR LOWER(p.city) LIKE ? ")
                   .append("OR LOWER(p.category_code) LIKE ? ")
                   .append(") ");
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
            }
        }

        if (params.category() != null && !params.category().isBlank()) {
            sql.append("AND p.category_code = ? ");
            args.add(params.category().trim().toUpperCase());
        }

        if (params.city() != null && !params.city().isBlank()) {
            sql.append("AND LOWER(p.city) = LOWER(?) ");
            args.add(params.city().trim());
        }

        if (params.state() != null && !params.state().isBlank()) {
            sql.append("AND LOWER(p.state) = LOWER(?) ");
            args.add(params.state().trim());
        }

        if (params.propertyType() != null && !params.propertyType().isBlank()) {
            sql.append("AND p.property_type = ? ");
            args.add(params.propertyType().trim().toUpperCase());
        }

        if (params.scope() != null && !params.scope().isBlank()) {
            sql.append("AND p.project_scope = ? ");
            args.add(params.scope().trim().toUpperCase());
        }

        if (params.professionalType() != null && !params.professionalType().isBlank()) {
            sql.append("AND s.professional_type = ? ");
            args.add(params.professionalType().trim().toUpperCase());
        }

        if (params.style() != null && !params.style().isBlank()) {
            sql.append("AND EXISTS (SELECT 1 FROM project_styles ps WHERE ps.project_id = p.id AND ps.style_code = ?) ");
            args.add(params.style().trim().toUpperCase());
        }
    }

    @Override
    public List<DiscoveryProfessionalCardDto> searchProfessionals(DiscoverySearchParams params) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        boolean hasQuery = params.q() != null && !params.q().trim().isEmpty();
        String rawQuery = hasQuery ? params.q().trim() : null;
        String queryPattern = hasQuery ? "%" + rawQuery.toLowerCase() + "%" : null;
        String queryExact = hasQuery ? rawQuery.toLowerCase() : null;

        sql.append("SELECT s.id, s.slug, s.name, s.professional_type, s.professional_title, s.tagline, ")
           .append("s.city, s.state, s.experience_since_year, s.created_at ");

        if (hasQuery) {
            if (isPostgreSQL()) {
                sql.append(", (CASE ")
                   .append("    WHEN LOWER(s.name) = ? THEN 100.0 ")
                   .append("    WHEN LOWER(s.name) LIKE ? THEN 80.0 ")
                   .append("    ELSE (ts_rank(to_tsvector('simple', s.name || ' ' || coalesce(s.tagline, '') || ' ' || coalesce(s.city, '')), plainto_tsquery('simple', ?)) * 40.0 + similarity(s.name, ?) * 40.0) ")
                   .append("END) as search_score ");
                args.add(queryExact);
                args.add(queryExact + "%");
                args.add(rawQuery);
                args.add(rawQuery);
            } else {
                sql.append(", (CASE ")
                   .append("    WHEN LOWER(s.name) = ? THEN 100 ")
                   .append("    WHEN LOWER(s.name) LIKE ? THEN 75 ")
                   .append("    WHEN LOWER(s.name) LIKE ? THEN 50 ")
                   .append("    WHEN LOWER(s.city) LIKE ? THEN 30 ")
                   .append("    ELSE 10 ")
                   .append("END) as search_score ");

                args.add(queryExact);
                args.add(queryExact + "%");
                args.add(queryPattern);
                args.add(queryPattern);
            }
        }

        sql.append("FROM designer_studios s ")
           .append("WHERE ").append(BASE_PUBLIC_STUDIO_GATE);

        applyProfessionalFilters(sql, args, params, rawQuery, queryPattern);

        // Sorting
        String sort = params.sort() != null ? params.sort().trim().toLowerCase() : (hasQuery ? "relevance" : "recent");
        if ("relevance".equals(sort) && hasQuery) {
            sql.append("ORDER BY search_score DESC, s.created_at DESC, s.id DESC ");
        } else if ("name".equals(sort)) {
            sql.append("ORDER BY s.name ASC ");
        } else {
            sql.append("ORDER BY s.created_at DESC, s.id DESC ");
        }

        sql.append("LIMIT ? OFFSET ? ");
        int limit = params.limit() != null ? Math.max(1, Math.min(params.limit(), 50)) : 20;
        int offset = params.offset() != null ? Math.max(0, Math.min(params.offset(), 1000)) : 0;
        args.add(limit);
        args.add(offset);

        List<StudioRow> rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new StudioRow(
                getUuid(rs, "id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("professional_type"),
                rs.getString("professional_title"),
                rs.getString("tagline"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getObject("experience_since_year") != null ? rs.getInt("experience_since_year") : null
        ), args.toArray());

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> studioIds = rows.stream().map(StudioRow::id).toList();

        // Batch fetch services, specialties, project counts, sample covers
        Map<UUID, List<String>> servicesByStudio = batchFetchServices(studioIds);
        Map<UUID, List<String>> specialtiesByStudio = batchFetchSpecialties(studioIds);
        Map<UUID, Integer> countsByStudio = batchFetchProjectCounts(studioIds);
        Map<UUID, List<String>> sampleCoversByStudio = batchFetchSampleCovers(studioIds);
        Map<UUID, Boolean> verifiedByStudio = batchFetchVerification(studioIds);
        Map<UUID, ReviewStats> reviewsByStudio = batchFetchReviewStats(studioIds);

        List<DiscoveryProfessionalCardDto> results = new ArrayList<>(rows.size());
        for (StudioRow r : rows) {
            String profTypeLabel = r.professionalType();
            try {
                profTypeLabel = ProfessionalType.fromCode(r.professionalType()).getDisplayName();
            } catch (Exception ignored) {}

            boolean isVerified = verifiedByStudio.getOrDefault(r.id(), false);
            ReviewStats stats = reviewsByStudio.get(r.id());
            Double reviewAvg = stats != null && stats.count() > 0 ? stats.average() : null;
            int reviewCount = stats != null ? stats.count() : 0;

            results.add(new DiscoveryProfessionalCardDto(
                    r.id(),
                    r.slug(),
                    r.name(),
                    r.professionalType(),
                    profTypeLabel,
                    r.professionalTitle(),
                    r.tagline(),
                    r.city(),
                    r.state(),
                    r.experienceSinceYear(),
                    servicesByStudio.getOrDefault(r.id(), Collections.emptyList()),
                    specialtiesByStudio.getOrDefault(r.id(), Collections.emptyList()),
                    countsByStudio.getOrDefault(r.id(), 0),
                    sampleCoversByStudio.getOrDefault(r.id(), Collections.emptyList()),
                    isVerified,
                    reviewAvg,
                    reviewCount
            ));
        }

        return results;
    }

    @Override
    public long countProfessionals(DiscoverySearchParams params) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        boolean hasQuery = params.q() != null && !params.q().trim().isEmpty();
        String rawQuery = hasQuery ? params.q().trim() : null;
        String queryPattern = hasQuery ? "%" + rawQuery.toLowerCase() + "%" : null;

        sql.append("SELECT COUNT(*) ")
           .append("FROM designer_studios s ")
           .append("WHERE ").append(BASE_PUBLIC_STUDIO_GATE);

        applyProfessionalFilters(sql, args, params, rawQuery, queryPattern);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count != null ? count : 0L;
    }

    private void applyProfessionalFilters(StringBuilder sql, List<Object> args, DiscoverySearchParams params, String rawQuery, String queryPattern) {
        if (queryPattern != null) {
            if (isPostgreSQL()) {
                sql.append("AND (")
                   .append("to_tsvector('simple', s.name || ' ' || coalesce(s.tagline, '') || ' ' || coalesce(s.city, '')) @@ plainto_tsquery('simple', ?) ")
                   .append("OR LOWER(s.name) LIKE ? ")
                   .append("OR s.name % ? ")
                   .append("OR similarity(s.name, ?) > 0.25 ")
                   .append("OR LOWER(s.tagline) LIKE ? ")
                   .append("OR LOWER(s.city) LIKE ? ")
                   .append("OR LOWER(s.professional_title) LIKE ? ")
                   .append(") ");
                args.add(rawQuery);
                args.add(queryPattern);
                args.add(rawQuery);
                args.add(rawQuery);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
            } else {
                sql.append("AND (")
                   .append("LOWER(s.name) LIKE ? ")
                   .append("OR LOWER(s.professional_title) LIKE ? ")
                   .append("OR LOWER(s.tagline) LIKE ? ")
                   .append("OR LOWER(s.city) LIKE ? ")
                   .append("OR LOWER(s.state) LIKE ? ")
                   .append(") ");
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
                args.add(queryPattern);
            }
        }

        if (params.city() != null && !params.city().isBlank()) {
            sql.append("AND (LOWER(s.city) = LOWER(?) OR EXISTS (")
               .append("    SELECT 1 FROM studio_service_areas sa WHERE sa.studio_id = s.id AND LOWER(sa.city_name) = LOWER(?)")
               .append(")) ");
            args.add(params.city().trim());
            args.add(params.city().trim());
        }

        if (params.state() != null && !params.state().isBlank()) {
            sql.append("AND LOWER(s.state) = LOWER(?) ");
            args.add(params.state().trim());
        }

        if (params.professionalType() != null && !params.professionalType().isBlank()) {
            sql.append("AND s.professional_type = ? ");
            args.add(params.professionalType().trim().toUpperCase());
        }

        if (params.style() != null && !params.style().isBlank()) {
            sql.append("AND EXISTS (SELECT 1 FROM studio_specialties sp WHERE sp.studio_id = s.id AND sp.specialty_code = ?) ");
            args.add(params.style().trim().toUpperCase());
        }
    }

    @Override
    public DiscoverySuggestionsResponse getSuggestions(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return new DiscoverySuggestionsResponse(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        String rawQuery = query.trim().toLowerCase();
        String pattern = "%" + rawQuery + "%";
        int safeLimit = Math.max(1, Math.min(limit, 10));

        // 1. Categories
        List<DiscoverySuggestionsResponse.SuggestionItem> categories = new ArrayList<>();
        for (ProjectCategory cat : ProjectCategory.values()) {
            if (cat.name().toLowerCase().contains(rawQuery) || cat.getDisplayName().toLowerCase().contains(rawQuery)) {
                categories.add(new DiscoverySuggestionsResponse.SuggestionItem(
                        cat.getDisplayName(),
                        "category",
                        cat.name().toLowerCase().replace('_', '-'),
                        cat.name()
                ));
                if (categories.size() >= safeLimit) break;
            }
        }

        // 2. Styles
        List<DiscoverySuggestionsResponse.SuggestionItem> styles = new ArrayList<>();
        for (ProjectStyle style : ProjectStyle.values()) {
            if (style.name().toLowerCase().contains(rawQuery) || style.getDisplayName().toLowerCase().contains(rawQuery)) {
                styles.add(new DiscoverySuggestionsResponse.SuggestionItem(
                        style.getDisplayName(),
                        "style",
                        style.name().toLowerCase().replace('_', '-'),
                        style.name()
                ));
                if (styles.size() >= safeLimit) break;
            }
        }

        // 3. Cities (from published projects/studios)
        String citySql = "SELECT DISTINCT p.city " +
                         "FROM studio_projects p " +
                         "JOIN designer_studios s ON s.id = p.studio_id " +
                         "LEFT JOIN studio_seo_settings seo ON seo.studio_id = s.id " +
                         "WHERE " + BASE_PUBLIC_PROJECT_GATE +
                         "AND p.city IS NOT NULL AND LOWER(p.city) LIKE ? " +
                         "LIMIT ?";
        List<DiscoverySuggestionsResponse.SuggestionItem> cities = jdbcTemplate.query(
                citySql,
                (rs, rowNum) -> {
                    String city = rs.getString("city");
                    return new DiscoverySuggestionsResponse.SuggestionItem(
                            city,
                            "city",
                            city.toLowerCase().replace(' ', '-'),
                            "City"
                    );
                },
                pattern,
                safeLimit
        );

        // 4. Studios
        String studioSql = "SELECT s.name, s.slug, s.city " +
                           "FROM designer_studios s " +
                           "LEFT JOIN studio_seo_settings seo ON seo.studio_id = s.id " +
                           "WHERE " + BASE_PUBLIC_STUDIO_GATE +
                           "AND LOWER(s.name) LIKE ? " +
                           "LIMIT ?";
        List<DiscoverySuggestionsResponse.SuggestionItem> studios = jdbcTemplate.query(
                studioSql,
                (rs, rowNum) -> new DiscoverySuggestionsResponse.SuggestionItem(
                        rs.getString("name"),
                        "studio",
                        rs.getString("slug"),
                        rs.getString("city")
                ),
                pattern,
                safeLimit
        );

        // 5. Projects
        String projectSql = "SELECT p.title, p.slug, s.slug as studio_slug, p.city " +
                            "FROM studio_projects p " +
                            "JOIN designer_studios s ON s.id = p.studio_id " +
                            "WHERE " + BASE_PUBLIC_PROJECT_GATE +
                            "AND LOWER(p.title) LIKE ? " +
                            "LIMIT ?";
        List<DiscoverySuggestionsResponse.SuggestionItem> projects = jdbcTemplate.query(
                projectSql,
                (rs, rowNum) -> new DiscoverySuggestionsResponse.SuggestionItem(
                        rs.getString("title"),
                        "project",
                        rs.getString("slug"),
                        rs.getString("studio_slug")
                ),
                pattern,
                safeLimit
        );

        return new DiscoverySuggestionsResponse(categories, cities, styles, studios, projects);
    }

    @Override
    public DiscoveryFacetsDto getFacets(DiscoverySearchParams params) {
        // 1. Categories facet
        String catSql = "SELECT p.category_code, COUNT(*) as cnt " +
                        "FROM studio_projects p " +
                        "JOIN designer_studios s ON s.id = p.studio_id " +
                        "WHERE " + BASE_PUBLIC_PROJECT_GATE +
                        "GROUP BY p.category_code " +
                        "ORDER BY cnt DESC";
        List<DiscoveryFacetItemDto> categories = jdbcTemplate.query(catSql, (rs, rowNum) -> {
            String code = rs.getString("category_code");
            String label = code;
            try {
                label = ProjectCategory.valueOf(code).getDisplayName();
            } catch (Exception ignored) {}
            return new DiscoveryFacetItemDto(code, label, rs.getLong("cnt"));
        });

        // 2. Cities facet
        String citySql = "SELECT p.city, COUNT(*) as cnt " +
                         "FROM studio_projects p " +
                         "JOIN designer_studios s ON s.id = p.studio_id " +
                         "WHERE " + BASE_PUBLIC_PROJECT_GATE +
                         "AND p.city IS NOT NULL AND TRIM(p.city) != '' " +
                         "GROUP BY p.city " +
                         "ORDER BY cnt DESC";
        List<DiscoveryFacetItemDto> cities = jdbcTemplate.query(citySql, (rs, rowNum) -> {
            String city = rs.getString("city");
            return new DiscoveryFacetItemDto(city, city, rs.getLong("cnt"));
        });

        // 3. Styles facet
        String styleSql = "SELECT ps.style_code, COUNT(*) as cnt " +
                          "FROM project_styles ps " +
                          "JOIN studio_projects p ON p.id = ps.project_id " +
                          "JOIN designer_studios s ON s.id = p.studio_id " +
                          "WHERE " + BASE_PUBLIC_PROJECT_GATE +
                          "GROUP BY ps.style_code " +
                          "ORDER BY cnt DESC";
        List<DiscoveryFacetItemDto> styles = jdbcTemplate.query(styleSql, (rs, rowNum) -> {
            String code = rs.getString("style_code");
            String label = code;
            try {
                label = ProjectStyle.valueOf(code).getDisplayName();
            } catch (Exception ignored) {}
            return new DiscoveryFacetItemDto(code, label, rs.getLong("cnt"));
        });

        // 4. Professional Types facet
        String profSql = "SELECT s.professional_type, COUNT(*) as cnt " +
                         "FROM designer_studios s " +
                         "WHERE " + BASE_PUBLIC_STUDIO_GATE +
                         "GROUP BY s.professional_type " +
                         "ORDER BY cnt DESC";
        List<DiscoveryFacetItemDto> profTypes = jdbcTemplate.query(profSql, (rs, rowNum) -> {
            String code = rs.getString("professional_type");
            String label = code;
            try {
                label = ProfessionalType.fromCode(code).getDisplayName();
            } catch (Exception ignored) {}
            return new DiscoveryFacetItemDto(code, label, rs.getLong("cnt"));
        });

        return new DiscoveryFacetsDto(categories, cities, styles, profTypes);
    }

    // ========================================================================
    // Batch Fetching Helpers (Zero N+1)
    // ========================================================================

    private Map<UUID, List<ProjectStyle>> batchFetchStyles(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT project_id, style_code FROM project_styles WHERE project_id IN (" + inSql + ") ORDER BY created_at ASC";

        Map<UUID, List<ProjectStyle>> map = new HashMap<>();
        for (UUID pid : projectIds) {
            map.put(pid, new ArrayList<>());
        }
        jdbcTemplate.query(sql, rs -> {
            UUID pid = getUuid(rs, "project_id");
            try {
                ProjectStyle style = ProjectStyle.valueOf(rs.getString("style_code"));
                map.computeIfAbsent(pid, k -> new ArrayList<>()).add(style);
            } catch (Exception ignored) {}
        }, projectIds.toArray());
        return map;
    }

    private record ProjectCoverInfo(String url, boolean isAiConcept) {}

    private Map<UUID, ProjectCoverInfo> batchFetchProjectCovers(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(projectIds.size(), "?"));

        String sql = "SELECT ma.project_id, ma.media_type, ma.is_cover, ma.sort_order, ma.created_at, " +
                     "md.public_url, md.variant_name " +
                     "FROM media_assets ma " +
                     "JOIN media_derivatives md ON md.media_id = ma.id AND md.studio_id = ma.studio_id " +
                     "WHERE ma.project_id IN (" + inSql + ") " +
                     "AND ma.deleted_at IS NULL " +
                     "AND ma.visibility != 'PRIVATE' " +
                     "AND ma.media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT') " +
                     "ORDER BY ma.project_id, " +
                     "ma.is_cover DESC, " +
                     "(CASE WHEN ma.media_type != 'AI_CONCEPT' THEN 1 ELSE 2 END) ASC, " +
                     "ma.sort_order ASC, ma.created_at ASC";

        Map<UUID, ProjectCoverInfo> map = new HashMap<>();
        // Priority for derivative variants: MEDIUM -> LARGE -> THUMBNAIL
        Map<UUID, Map<String, String>> variantsByProject = new HashMap<>();
        Map<UUID, String> mediaTypeByProject = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            UUID pid = getUuid(rs, "project_id");
            String variant = rs.getString("variant_name");
            String url = rs.getString("public_url");
            String mediaType = rs.getString("media_type");

            if (!variantsByProject.containsKey(pid)) {
                variantsByProject.put(pid, new HashMap<>());
                mediaTypeByProject.put(pid, mediaType);
            }
            variantsByProject.get(pid).put(variant, url);
        }, projectIds.toArray());

        for (Map.Entry<UUID, Map<String, String>> entry : variantsByProject.entrySet()) {
            UUID pid = entry.getKey();
            Map<String, String> vars = entry.getValue();
            String chosenUrl = vars.get("MEDIUM");
            if (chosenUrl == null) chosenUrl = vars.get("LARGE");
            if (chosenUrl == null) chosenUrl = vars.get("THUMBNAIL");

            if (chosenUrl != null) {
                boolean isAiConcept = "AI_CONCEPT".equalsIgnoreCase(mediaTypeByProject.get(pid));
                map.put(pid, new ProjectCoverInfo(chosenUrl, isAiConcept));
            }
        }

        return map;
    }

    private Map<UUID, List<String>> batchFetchServices(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));
        String sql = "SELECT studio_id, service_name FROM studio_services WHERE studio_id IN (" + inSql + ")";

        Map<UUID, List<String>> map = new HashMap<>();
        for (UUID sid : studioIds) {
            map.put(sid, new ArrayList<>());
        }
        jdbcTemplate.query(sql, rs -> {
            UUID sid = getUuid(rs, "studio_id");
            map.computeIfAbsent(sid, k -> new ArrayList<>()).add(rs.getString("service_name"));
        }, studioIds.toArray());
        return map;
    }

    private Map<UUID, List<String>> batchFetchSpecialties(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));
        String sql = "SELECT studio_id, specialty_name FROM studio_specialties WHERE studio_id IN (" + inSql + ")";

        Map<UUID, List<String>> map = new HashMap<>();
        for (UUID sid : studioIds) {
            map.put(sid, new ArrayList<>());
        }
        jdbcTemplate.query(sql, rs -> {
            UUID sid = getUuid(rs, "studio_id");
            map.computeIfAbsent(sid, k -> new ArrayList<>()).add(rs.getString("specialty_name"));
        }, studioIds.toArray());
        return map;
    }

    private Map<UUID, Integer> batchFetchProjectCounts(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));
        String sql = "SELECT studio_id, COUNT(*) as cnt FROM studio_projects " +
                     "WHERE project_status = 'READY' AND visibility_status = 'PORTFOLIO' AND archived_at IS NULL " +
                     "AND studio_id IN (" + inSql + ") GROUP BY studio_id";

        Map<UUID, Integer> map = new HashMap<>();
        for (UUID sid : studioIds) {
            map.put(sid, 0);
        }
        jdbcTemplate.query(sql, rs -> {
            UUID sid = getUuid(rs, "studio_id");
            map.put(sid, rs.getInt("cnt"));
        }, studioIds.toArray());
        return map;
    }

    private Map<UUID, List<String>> batchFetchSampleCovers(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));

        String sql = "SELECT p.studio_id, p.id as project_id, md.public_url " +
                     "FROM studio_projects p " +
                     "JOIN media_assets ma ON ma.project_id = p.id AND ma.studio_id = p.studio_id " +
                     "JOIN media_derivatives md ON md.media_id = ma.id AND md.studio_id = ma.studio_id " +
                     "WHERE p.studio_id IN (" + inSql + ") " +
                     "AND p.project_status = 'READY' " +
                     "AND p.visibility_status = 'PORTFOLIO' " +
                     "AND p.archived_at IS NULL " +
                     "AND ma.deleted_at IS NULL " +
                     "AND ma.visibility != 'PRIVATE' " +
                     "AND ma.media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT') " +
                     "AND md.variant_name IN ('MEDIUM', 'LARGE', 'THUMBNAIL') " +
                     "ORDER BY p.studio_id, p.display_order ASC, p.created_at DESC, ma.is_cover DESC";

        Map<UUID, List<String>> map = new HashMap<>();
        for (UUID sid : studioIds) {
            map.put(sid, new ArrayList<>());
        }
        Map<UUID, Set<UUID>> seenProjects = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            UUID sid = getUuid(rs, "studio_id");
            UUID pid = getUuid(rs, "project_id");
            String url = rs.getString("public_url");

            List<String> list = map.computeIfAbsent(sid, k -> new ArrayList<>());
            Set<UUID> seen = seenProjects.computeIfAbsent(sid, k -> new HashSet<>());

            if (list.size() < 3 && !seen.contains(pid) && url != null) {
                seen.add(pid);
                list.add(url);
            }
        }, studioIds.toArray());

        return map;
    }

    private record ReviewStats(double average, int count) {}

    private Map<UUID, Boolean> batchFetchVerification(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));
        String sql = "SELECT studio_id FROM studio_verifications WHERE studio_id IN (" + inSql + ") " +
                     "AND status = 'VERIFIED' AND (expires_at IS NULL OR expires_at > now())";
        Map<UUID, Boolean> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(getUuid(rs, "studio_id"), true);
        }, studioIds.toArray());
        return map;
    }

    private Map<UUID, ReviewStats> batchFetchReviewStats(List<UUID> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(studioIds.size(), "?"));
        String sql = "SELECT studio_id, AVG(rating) as avg_rating, count(*) as count_reviews " +
                     "FROM studio_reviews WHERE studio_id IN (" + inSql + ") " +
                     "AND status = 'PUBLISHED' GROUP BY studio_id";
        Map<UUID, ReviewStats> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            UUID sid = getUuid(rs, "studio_id");
            double avg = rs.getDouble("avg_rating");
            double rounded = Math.round(avg * 10.0) / 10.0;
            int count = rs.getInt("count_reviews");
            map.put(sid, new ReviewStats(rounded, count));
        }, studioIds.toArray());
        return map;
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private record ProjectRow(
            UUID id,
            String slug,
            String title,
            String shortDescription,
            String categoryCode,
            String city,
            String state,
            String propertyType,
            String projectScope,
            Integer completionYear,
            UUID studioId,
            String studioSlug,
            String studioName,
            String professionalType
    ) {}

    private record StudioRow(
            UUID id,
            String slug,
            String name,
            String professionalType,
            String professionalTitle,
            String tagline,
            String city,
            String state,
            Integer experienceSinceYear
    ) {}
}
