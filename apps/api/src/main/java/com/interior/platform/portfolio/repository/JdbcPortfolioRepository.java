package com.interior.platform.portfolio.repository;

import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioSectionRecord;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.domain.PortfolioVersionRecord;
import com.interior.platform.portfolio.domain.SectionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPortfolioRepository implements PortfolioRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PortfolioRecord> portfolioMapper = (rs, rowNum) -> new PortfolioRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            PortfolioTemplateKey.valueOf(rs.getString("template_key")),
            PortfolioStatus.valueOf(rs.getString("status")),
            rs.getString("headline"),
            rs.getString("subheadline"),
            rs.getString("bio"),
            rs.getString("design_philosophy"),
            rs.getObject("years_of_experience") != null ? rs.getInt("years_of_experience") : null,
            rs.getString("primary_color"),
            rs.getString("secondary_color"),
            rs.getString("accent_color"),
            FontPairing.valueOf(rs.getString("font_pairing")),
            rs.getLong("version"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final RowMapper<PortfolioSectionRecord> sectionMapper = (rs, rowNum) -> new PortfolioSectionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "portfolio_id"),
            getUuid(rs, "studio_id"),
            SectionType.valueOf(rs.getString("section_type")),
            rs.getInt("display_order"),
            rs.getBoolean("is_visible"),
            rs.getInt("schema_version"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final RowMapper<PortfolioVersionRecord> versionMapper = (rs, rowNum) -> new PortfolioVersionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "portfolio_id"),
            getUuid(rs, "studio_id"),
            rs.getInt("version_number"),
            rs.getString("label"),
            rs.getString("snapshot_payload"),
            getUuid(rs, "created_by"),
            rs.getTimestamp("created_at").toInstant()
    );

    @Override
    public Optional<PortfolioRecord> findPortfolioByStudioId(UUID studioId) {
        String sql = "SELECT id, studio_id, template_key, status, headline, subheadline, bio, design_philosophy, " +
                     "years_of_experience, primary_color, secondary_color, accent_color, font_pairing, " +
                     "version, created_at, updated_at FROM portfolios WHERE studio_id = ?";
        List<PortfolioRecord> results = jdbcTemplate.query(sql, portfolioMapper, studioId);
        return results.stream().findFirst();
    }

    @Override
    public Optional<PortfolioRecord> findPortfolioById(UUID portfolioId) {
        String sql = "SELECT id, studio_id, template_key, status, headline, subheadline, bio, design_philosophy, " +
                     "years_of_experience, primary_color, secondary_color, accent_color, font_pairing, " +
                     "version, created_at, updated_at FROM portfolios WHERE id = ?";
        List<PortfolioRecord> results = jdbcTemplate.query(sql, portfolioMapper, portfolioId);
        return results.stream().findFirst();
    }

    @Override
    public PortfolioRecord createPortfolio(PortfolioRecord portfolio) {
        String sql = "INSERT INTO portfolios (" +
                     "id, studio_id, template_key, status, headline, subheadline, bio, design_philosophy, " +
                     "years_of_experience, primary_color, secondary_color, accent_color, font_pairing, " +
                     "version, created_at, updated_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";
        jdbcTemplate.update(sql,
                portfolio.id(),
                portfolio.studioId(),
                portfolio.templateKey().name(),
                portfolio.status().name(),
                portfolio.headline(),
                portfolio.subheadline(),
                portfolio.bio(),
                portfolio.designPhilosophy(),
                portfolio.yearsOfExperience(),
                portfolio.primaryColor(),
                portfolio.secondaryColor(),
                portfolio.accentColor(),
                portfolio.fontPairing().name(),
                portfolio.version()
        );
        return findPortfolioById(portfolio.id()).orElseThrow();
    }

    @Override
    public boolean updatePortfolio(PortfolioRecord portfolio, long expectedVersion) {
        String sql = "UPDATE portfolios SET " +
                     "template_key = ?, headline = ?, subheadline = ?, bio = ?, design_philosophy = ?, years_of_experience = ?, " +
                     "primary_color = ?, secondary_color = ?, accent_color = ?, font_pairing = ?, status = ?, " +
                     "version = version + 1, updated_at = now() " +
                     "WHERE id = ? AND version = ?";
        int updated = jdbcTemplate.update(sql,
                portfolio.templateKey().name(),
                portfolio.headline(),
                portfolio.subheadline(),
                portfolio.bio(),
                portfolio.designPhilosophy(),
                portfolio.yearsOfExperience(),
                portfolio.primaryColor(),
                portfolio.secondaryColor(),
                portfolio.accentColor(),
                portfolio.fontPairing().name(),
                portfolio.status().name(),
                portfolio.id(),
                expectedVersion
        );
        return updated > 0;
    }

    @Override
    public boolean incrementVersion(UUID portfolioId, long expectedVersion) {
        String sql = "UPDATE portfolios SET version = version + 1, updated_at = now() WHERE id = ? AND version = ?";
        int updated = jdbcTemplate.update(sql, portfolioId, expectedVersion);
        return updated > 0;
    }

    @Override
    public boolean updateTemplateKey(UUID portfolioId, String templateKey, long expectedVersion) {
        String sql = "UPDATE portfolios SET template_key = ?, version = version + 1, updated_at = now() " +
                     "WHERE id = ? AND version = ?";
        int updated = jdbcTemplate.update(sql, templateKey, portfolioId, expectedVersion);
        return updated > 0;
    }

    @Override
    public List<PortfolioSectionRecord> findSectionsByPortfolioId(UUID portfolioId) {
        String sql = "SELECT id, portfolio_id, studio_id, section_type, display_order, is_visible, " +
                     "schema_version, content, created_at, updated_at " +
                     "FROM portfolio_sections WHERE portfolio_id = ? ORDER BY display_order ASC";
        return jdbcTemplate.query(sql, sectionMapper, portfolioId);
    }

    @Override
    public Optional<PortfolioSectionRecord> findSectionById(UUID sectionId, UUID portfolioId) {
        String sql = "SELECT id, portfolio_id, studio_id, section_type, display_order, is_visible, " +
                     "schema_version, content, created_at, updated_at " +
                     "FROM portfolio_sections WHERE id = ? AND portfolio_id = ?";
        List<PortfolioSectionRecord> results = jdbcTemplate.query(sql, sectionMapper, sectionId, portfolioId);
        return results.stream().findFirst();
    }

    @Override
    public void createSections(List<PortfolioSectionRecord> sections) {
        String sql = "INSERT INTO portfolio_sections (" +
                     "id, portfolio_id, studio_id, section_type, display_order, is_visible, " +
                     "schema_version, content, created_at, updated_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())";
        for (PortfolioSectionRecord s : sections) {
            jdbcTemplate.update(sql,
                    s.id(),
                    s.portfolioId(),
                    s.studioId(),
                    s.sectionType().name(),
                    s.displayOrder(),
                    s.isVisible(),
                    s.schemaVersion(),
                    s.content()
            );
        }
    }

    @Override
    public void updateSection(PortfolioSectionRecord section) {
        String sql = "UPDATE portfolio_sections SET " +
                     "is_visible = ?, content = ?, updated_at = now() " +
                     "WHERE id = ? AND portfolio_id = ?";
        jdbcTemplate.update(sql,
                section.isVisible(),
                section.content(),
                section.id(),
                section.portfolioId()
        );
    }

    @Override
    public void reorderSections(UUID portfolioId, List<UUID> orderedSectionIds) {
        for (int i = 0; i < orderedSectionIds.size(); i++) {
            jdbcTemplate.update(
                    "UPDATE portfolio_sections SET display_order = ? WHERE id = ? AND portfolio_id = ?",
                    10000 + i, orderedSectionIds.get(i), portfolioId
            );
        }
        for (int i = 0; i < orderedSectionIds.size(); i++) {
            jdbcTemplate.update(
                    "UPDATE portfolio_sections SET display_order = ? WHERE id = ? AND portfolio_id = ?",
                    i, orderedSectionIds.get(i), portfolioId
            );
        }
    }

    @Override
    public void replaceAllSections(UUID portfolioId, UUID studioId, List<PortfolioSectionRecord> newSections) {
        jdbcTemplate.update("DELETE FROM portfolio_sections WHERE portfolio_id = ?", portfolioId);
        createSections(newSections);
    }

    @Override
    public void createVersionSnapshot(PortfolioVersionRecord versionRecord) {
        String sql = "INSERT INTO portfolio_versions (" +
                     "id, portfolio_id, studio_id, version_number, label, snapshot_payload, created_by, created_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, now())";
        jdbcTemplate.update(sql,
                versionRecord.id(),
                versionRecord.portfolioId(),
                versionRecord.studioId(),
                versionRecord.versionNumber(),
                versionRecord.label(),
                versionRecord.snapshotPayload(),
                versionRecord.createdBy()
        );
    }

    @Override
    public int getNextVersionNumber(UUID portfolioId) {
        String sql = "SELECT COALESCE(MAX(version_number), 0) + 1 FROM portfolio_versions WHERE portfolio_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, portfolioId);
        return next != null ? next : 1;
    }

    @Override
    public List<PortfolioVersionRecord> findVersionsByPortfolioId(UUID portfolioId, int limit) {
        String sql = "SELECT id, portfolio_id, studio_id, version_number, label, snapshot_payload, created_by, created_at " +
                     "FROM portfolio_versions WHERE portfolio_id = ? ORDER BY version_number DESC LIMIT ?";
        return jdbcTemplate.query(sql, versionMapper, portfolioId, limit);
    }

    @Override
    public Optional<PortfolioVersionRecord> findVersionByNumber(UUID portfolioId, int versionNumber) {
        String sql = "SELECT id, portfolio_id, studio_id, version_number, label, snapshot_payload, created_by, created_at " +
                     "FROM portfolio_versions WHERE portfolio_id = ? AND version_number = ?";
        List<PortfolioVersionRecord> results = jdbcTemplate.query(sql, versionMapper, portfolioId, versionNumber);
        return results.stream().findFirst();
    }

    @Override
    public void pruneOldVersions(UUID portfolioId, int retainMaxCount) {
        String sql = "DELETE FROM portfolio_versions WHERE portfolio_id = ? AND id NOT IN (" +
                     "SELECT id FROM portfolio_versions WHERE portfolio_id = ? ORDER BY version_number DESC LIMIT ?" +
                     ")";
        jdbcTemplate.update(sql, portfolioId, portfolioId, retainMaxCount);
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }
}
