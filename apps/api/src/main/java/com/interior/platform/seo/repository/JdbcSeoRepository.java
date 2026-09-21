package com.interior.platform.seo.repository;

import com.interior.platform.seo.domain.SeoSettingsRecord;
import com.interior.platform.seo.dto.SitemapItemDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSeoRepository implements SeoRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSeoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<SeoSettingsRecord> settingsRowMapper = (rs, rowNum) -> new SeoSettingsRecord(
            (UUID) rs.getObject("id"),
            (UUID) rs.getObject("studio_id"),
            rs.getString("meta_title_override"),
            rs.getString("meta_description_override"),
            rs.getString("canonical_url_override"),
            rs.getBoolean("indexing_enabled"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    @Override
    public Optional<SeoSettingsRecord> findByStudioId(UUID studioId) {
        String sql = "SELECT id, studio_id, meta_title_override, meta_description_override, canonical_url_override, " +
                     "indexing_enabled, created_at, updated_at FROM studio_seo_settings WHERE studio_id = ?";
        List<SeoSettingsRecord> list = jdbcTemplate.query(sql, settingsRowMapper, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public void upsert(SeoSettingsRecord settings) {
        if (findByStudioId(settings.studioId()).isPresent()) {
            String updateSql = "UPDATE studio_seo_settings SET " +
                               "meta_title_override = ?, " +
                               "meta_description_override = ?, " +
                               "canonical_url_override = ?, " +
                               "indexing_enabled = ?, " +
                               "updated_at = now() " +
                               "WHERE studio_id = ?";
            jdbcTemplate.update(updateSql,
                    settings.metaTitleOverride(),
                    settings.metaDescriptionOverride(),
                    settings.canonicalUrlOverride(),
                    settings.indexingEnabled(),
                    settings.studioId()
            );
        } else {
            String insertSql = "INSERT INTO studio_seo_settings (id, studio_id, meta_title_override, meta_description_override, " +
                               "canonical_url_override, indexing_enabled, created_at, updated_at) " +
                               "VALUES (?, ?, ?, ?, ?, ?, now(), now())";
            jdbcTemplate.update(insertSql,
                    settings.id(),
                    settings.studioId(),
                    settings.metaTitleOverride(),
                    settings.metaDescriptionOverride(),
                    settings.canonicalUrlOverride(),
                    settings.indexingEnabled()
            );
        }
    }

    @Override
    public void updatePublicationStatus(UUID studioId, String status, Instant publishedAt) {
        String sql = "UPDATE designer_studios SET publication_status = ?, published_at = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status, publishedAt != null ? Timestamp.from(publishedAt) : null, studioId);
    }

    @Override
    public List<SitemapItemDto> getPublishedStudioSitemapItems() {
        String sql = "SELECT s.slug, s.updated_at, s.published_at " +
                     "FROM designer_studios s " +
                     "LEFT JOIN studio_seo_settings seo ON seo.studio_id = s.id " +
                     "WHERE s.publication_status = 'PUBLISHED' " +
                     "AND (seo.indexing_enabled IS NULL OR seo.indexing_enabled = true)";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String slug = rs.getString("slug");
            Timestamp updated = rs.getTimestamp("updated_at");
            return new SitemapItemDto(
                    "/professionals/" + slug,
                    "0.9",
                    "weekly",
                    updated != null ? updated.toInstant() : Instant.now(),
                    null,
                    null
            );
        });
    }

    @Override
    public List<SitemapItemDto> getPublicProjectSitemapItems() {
        String sql = "SELECT p.slug AS project_slug, s.slug AS studio_slug, p.title AS project_title, p.updated_at, " +
                     "d.public_url AS cover_url " +
                     "FROM studio_projects p " +
                     "JOIN designer_studios s ON s.id = p.studio_id " +
                     "LEFT JOIN studio_seo_settings seo ON seo.studio_id = s.id " +
                     "LEFT JOIN media_assets ma ON ma.project_id = p.id AND ma.is_cover = true AND ma.processing_status = 'READY' " +
                     "LEFT JOIN media_derivatives d ON d.media_id = ma.id AND d.variant_name = 'LARGE' " +
                     "WHERE s.publication_status = 'PUBLISHED' " +
                     "AND (seo.indexing_enabled IS NULL OR seo.indexing_enabled = true) " +
                     "AND p.project_status = 'READY' " +
                     "AND p.visibility_status IN ('PORTFOLIO', 'PUBLIC') " +
                     "AND p.archived_at IS NULL";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String projectSlug = rs.getString("project_slug");
            String title = rs.getString("project_title");
            Timestamp updated = rs.getTimestamp("updated_at");
            String coverUrl = rs.getString("cover_url");

            return new SitemapItemDto(
                    "/projects/" + projectSlug,
                    "0.8",
                    "monthly",
                    updated != null ? updated.toInstant() : Instant.now(),
                    coverUrl,
                    title
            );
        });
    }
}
