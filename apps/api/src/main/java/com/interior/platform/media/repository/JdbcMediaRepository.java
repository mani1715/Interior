package com.interior.platform.media.repository;

import com.interior.platform.media.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMediaRepository implements MediaRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMediaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudioWatermarkSettingsRecord> watermarkSettingsMapper = (rs, rowNum) -> new StudioWatermarkSettingsRecord(
            getUuid(rs, "studio_id"),
            rs.getBoolean("enabled"),
            WatermarkPosition.valueOf(rs.getString("position")),
            rs.getBigDecimal("opacity"),
            rs.getInt("size_percentage"),
            rs.getBoolean("use_logo"),
            rs.getString("fallback_text"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final RowMapper<UploadIntentRecord> uploadIntentMapper = (rs, rowNum) -> new UploadIntentRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            MediaType.valueOf(rs.getString("media_type")),
            rs.getString("expected_content_type"),
            rs.getLong("expected_size_bytes"),
            rs.getString("quarantine_key"),
            UploadIntentStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("expires_at").toInstant(),
            getUuid(rs, "created_by"),
            rs.getTimestamp("created_at").toInstant()
    );

    private final RowMapper<MediaAssetRecord> mediaAssetMapper = (rs, rowNum) -> new MediaAssetRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            MediaType.valueOf(rs.getString("media_type")),
            MediaVisibility.valueOf(rs.getString("visibility")),
            MediaProcessingStatus.valueOf(rs.getString("processing_status")),
            rs.getString("original_storage_key"),
            rs.getString("content_type"),
            rs.getLong("file_size"),
            rs.getInt("width"),
            rs.getInt("height"),
            rs.getInt("sort_order"),
            rs.getBoolean("is_cover"),
            rs.getString("alt_text"),
            rs.getString("caption"),
            rs.getBoolean("watermark_enabled"),
            getUuid(rs, "created_by"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toInstant() : null
    );

    private final RowMapper<MediaDerivativeRecord> derivativeMapper = (rs, rowNum) -> new MediaDerivativeRecord(
            getUuid(rs, "id"),
            getUuid(rs, "media_id"),
            getUuid(rs, "studio_id"),
            DerivativeVariant.valueOf(rs.getString("variant_name")),
            rs.getInt("width"),
            rs.getInt("height"),
            rs.getString("format"),
            rs.getLong("file_size"),
            rs.getString("storage_key"),
            rs.getString("public_url"),
            rs.getBoolean("is_watermarked"),
            rs.getTimestamp("created_at").toInstant()
    );

    // 1. Watermark Settings
    @Override
    public Optional<StudioWatermarkSettingsRecord> findWatermarkSettings(UUID studioId) {
        String sql = "SELECT * FROM studio_watermark_settings WHERE studio_id = ?";
        List<StudioWatermarkSettingsRecord> list = jdbcTemplate.query(sql, watermarkSettingsMapper, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public StudioWatermarkSettingsRecord saveWatermarkSettings(StudioWatermarkSettingsRecord s) {
        Optional<StudioWatermarkSettingsRecord> existing = findWatermarkSettings(s.studioId());
        Instant now = Instant.now();
        if (existing.isPresent()) {
            String sql = "UPDATE studio_watermark_settings SET " +
                         "enabled = ?, position = ?, opacity = ?, size_percentage = ?, use_logo = ?, fallback_text = ?, updated_at = ? " +
                         "WHERE studio_id = ?";
            jdbcTemplate.update(sql,
                    s.enabled(),
                    s.position().name(),
                    s.opacity(),
                    s.sizePercentage(),
                    s.useLogo(),
                    s.fallbackText(),
                    Timestamp.from(now),
                    s.studioId()
            );
            return new StudioWatermarkSettingsRecord(
                    s.studioId(),
                    s.enabled(),
                    s.position(),
                    s.opacity(),
                    s.sizePercentage(),
                    s.useLogo(),
                    s.fallbackText(),
                    existing.get().createdAt(),
                    now
            );
        } else {
            String sql = "INSERT INTO studio_watermark_settings (" +
                         "studio_id, enabled, position, opacity, size_percentage, use_logo, fallback_text, created_at, updated_at" +
                         ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql,
                    s.studioId(),
                    s.enabled(),
                    s.position().name(),
                    s.opacity(),
                    s.sizePercentage(),
                    s.useLogo(),
                    s.fallbackText(),
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
            return new StudioWatermarkSettingsRecord(
                    s.studioId(),
                    s.enabled(),
                    s.position(),
                    s.opacity(),
                    s.sizePercentage(),
                    s.useLogo(),
                    s.fallbackText(),
                    now,
                    now
            );
        }
    }

    // 2. Upload Intents
    @Override
    public UploadIntentRecord createUploadIntent(UploadIntentRecord intent) {
        String sql = "INSERT INTO upload_intents (" +
                     "id, studio_id, project_id, media_type, expected_content_type, expected_size_bytes, " +
                     "quarantine_key, status, expires_at, created_by, created_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                intent.id(),
                intent.studioId(),
                intent.projectId(),
                intent.mediaType().name(),
                intent.expectedContentType(),
                intent.expectedSizeBytes(),
                intent.quarantineKey(),
                intent.status().name(),
                Timestamp.from(intent.expiresAt()),
                intent.createdBy(),
                Timestamp.from(intent.createdAt())
        );
        return intent;
    }

    @Override
    public Optional<UploadIntentRecord> findUploadIntent(UUID intentId, UUID studioId) {
        String sql = "SELECT * FROM upload_intents WHERE id = ? AND studio_id = ?";
        List<UploadIntentRecord> list = jdbcTemplate.query(sql, uploadIntentMapper, intentId, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<UploadIntentRecord> findUploadIntentGlobal(UUID intentId) {
        String sql = "SELECT * FROM upload_intents WHERE id = ?";
        List<UploadIntentRecord> list = jdbcTemplate.query(sql, uploadIntentMapper, intentId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public void updateUploadIntentStatus(UUID intentId, UploadIntentStatus status) {
        String sql = "UPDATE upload_intents SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), intentId);
    }

    // 3. Media Assets
    @Override
    public MediaAssetRecord createMediaAsset(MediaAssetRecord asset) {
        String sql = "INSERT INTO media_assets (" +
                     "id, studio_id, project_id, media_type, visibility, processing_status, " +
                     "original_storage_key, content_type, file_size, width, height, sort_order, " +
                     "is_cover, alt_text, caption, watermark_enabled, created_by, created_at, updated_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                asset.id(),
                asset.studioId(),
                asset.projectId(),
                asset.mediaType().name(),
                asset.visibility().name(),
                asset.processingStatus().name(),
                asset.originalStorageKey(),
                asset.contentType(),
                asset.fileSize(),
                asset.width(),
                asset.height(),
                asset.sortOrder(),
                asset.isCover(),
                asset.altText(),
                asset.caption(),
                asset.watermarkEnabled(),
                asset.createdBy(),
                Timestamp.from(asset.createdAt()),
                Timestamp.from(asset.updatedAt())
        );
        return asset;
    }

    @Override
    public Optional<MediaAssetRecord> findMediaAsset(UUID mediaId, UUID studioId) {
        String sql = "SELECT * FROM media_assets WHERE id = ? AND studio_id = ?";
        List<MediaAssetRecord> list = jdbcTemplate.query(sql, mediaAssetMapper, mediaId, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<MediaAssetRecord> findMediaAssetsByProject(UUID projectId, UUID studioId, boolean includeDeleted) {
        String sql = "SELECT * FROM media_assets WHERE project_id = ? AND studio_id = ? " +
                     (includeDeleted ? "" : "AND deleted_at IS NULL ") +
                     "ORDER BY sort_order ASC, created_at ASC";
        return jdbcTemplate.query(sql, mediaAssetMapper, projectId, studioId);
    }

    @Override
    public List<MediaAssetRecord> findMediaAssetsByStudio(
            UUID studioId,
            UUID projectId,
            MediaType mediaType,
            MediaVisibility visibility,
            MediaProcessingStatus status,
            boolean includeDeleted
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM media_assets WHERE studio_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(studioId);

        if (!includeDeleted) {
            sql.append("AND deleted_at IS NULL ");
        }
        if (projectId != null) {
            sql.append("AND project_id = ? ");
            params.add(projectId);
        }
        if (mediaType != null) {
            sql.append("AND media_type = ? ");
            params.add(mediaType.name());
        }
        if (visibility != null) {
            sql.append("AND visibility = ? ");
            params.add(visibility.name());
        }
        if (status != null) {
            sql.append("AND processing_status = ? ");
            params.add(status.name());
        }

        sql.append("ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), mediaAssetMapper, params.toArray());
    }

    @Override
    public Optional<MediaAssetRecord> findCoverMedia(UUID projectId, UUID studioId) {
        String sql = "SELECT * FROM media_assets WHERE project_id = ? AND studio_id = ? AND is_cover = true AND deleted_at IS NULL LIMIT 1";
        List<MediaAssetRecord> list = jdbcTemplate.query(sql, mediaAssetMapper, projectId, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public void updateMediaAsset(MediaAssetRecord asset) {
        String sql = "UPDATE media_assets SET " +
                     "visibility = ?, processing_status = ?, is_cover = ?, alt_text = ?, caption = ?, " +
                     "watermark_enabled = ?, sort_order = ?, updated_at = ? " +
                     "WHERE id = ? AND studio_id = ?";
        jdbcTemplate.update(sql,
                asset.visibility().name(),
                asset.processingStatus().name(),
                asset.isCover(),
                asset.altText(),
                asset.caption(),
                asset.watermarkEnabled(),
                asset.sortOrder(),
                Timestamp.from(Instant.now()),
                asset.id(),
                asset.studioId()
        );
    }

    @Override
    public void unsetOtherCovers(UUID projectId, UUID studioId, UUID keepCoverMediaId) {
        String sql = "UPDATE media_assets SET is_cover = false, updated_at = ? WHERE project_id = ? AND studio_id = ? AND id != ?";
        jdbcTemplate.update(sql, Timestamp.from(Instant.now()), projectId, studioId, keepCoverMediaId);
    }

    @Override
    public void updateSortOrder(UUID mediaId, UUID studioId, int sortOrder) {
        String sql = "UPDATE media_assets SET sort_order = ?, updated_at = ? WHERE id = ? AND studio_id = ?";
        jdbcTemplate.update(sql, sortOrder, Timestamp.from(Instant.now()), mediaId, studioId);
    }

    @Override
    public void softDeleteMediaAsset(UUID mediaId, UUID studioId) {
        String sql = "UPDATE media_assets SET deleted_at = ?, processing_status = 'DELETED', updated_at = ? WHERE id = ? AND studio_id = ?";
        Instant now = Instant.now();
        jdbcTemplate.update(sql, Timestamp.from(now), Timestamp.from(now), mediaId, studioId);
    }

    @Override
    public int getNextSortOrder(UUID projectId, UUID studioId) {
        String sql = "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM media_assets WHERE project_id = ? AND studio_id = ? AND deleted_at IS NULL";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, projectId, studioId);
        return next != null ? next : 0;
    }

    @Override
    public long countActiveMediaByStudio(UUID studioId) {
        String sql = "SELECT COUNT(*) FROM media_assets WHERE studio_id = ? AND deleted_at IS NULL";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, studioId);
        return count != null ? count : 0L;
    }

    // 4. Media Derivatives
    @Override
    public void saveDerivatives(List<MediaDerivativeRecord> derivatives) {
        String sql = "INSERT INTO media_derivatives (" +
                     "id, media_id, studio_id, variant_name, width, height, format, file_size, storage_key, public_url, is_watermarked, created_at" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        for (MediaDerivativeRecord d : derivatives) {
            jdbcTemplate.update(sql,
                    d.id(),
                    d.mediaId(),
                    d.studioId(),
                    d.variantName().name(),
                    d.width(),
                    d.height(),
                    d.format(),
                    d.fileSize(),
                    d.storageKey(),
                    d.publicUrl(),
                    d.isWatermarked(),
                    Timestamp.from(d.createdAt())
            );
        }
    }

    @Override
    public List<MediaDerivativeRecord> findDerivativesByMediaId(UUID mediaId, UUID studioId) {
        String sql = "SELECT * FROM media_derivatives WHERE media_id = ? AND studio_id = ? ORDER BY width ASC";
        return jdbcTemplate.query(sql, derivativeMapper, mediaId, studioId);
    }

    @Override
    public void deleteDerivativesByMediaId(UUID mediaId, UUID studioId) {
        String sql = "DELETE FROM media_derivatives WHERE media_id = ? AND studio_id = ?";
        jdbcTemplate.update(sql, mediaId, studioId);
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }
}
