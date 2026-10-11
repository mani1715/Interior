package com.interior.platform.privacy.repository;

import com.interior.platform.privacy.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPrivacyRepository implements PrivacyRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPrivacyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PrivacyRequestRecord> requestRowMapper = (rs, rowNum) -> new PrivacyRequestRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("studio_id", UUID.class),
            PrivacyRequestType.valueOf(rs.getString("request_type")),
            PrivacyRequestStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("confirmed_at")),
            toInstant(rs.getTimestamp("completed_at")),
            rs.getString("failure_reason"),
            rs.getString("metadata"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    private final RowMapper<DataExportJobRecord> exportJobRowMapper = (rs, rowNum) -> new DataExportJobRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("privacy_request_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("studio_id", UUID.class),
            ExportType.valueOf(rs.getString("export_type")),
            rs.getString("storage_key"),
            rs.getObject("file_size_bytes") != null ? rs.getLong("file_size_bytes") : null,
            rs.getString("download_token_hash"),
            rs.getInt("download_count"),
            DataExportStatus.valueOf(rs.getString("status")),
            toInstant(rs.getTimestamp("expires_at")),
            toInstant(rs.getTimestamp("completed_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    private final RowMapper<PrivacyLegalHoldRecord> legalHoldRowMapper = (rs, rowNum) -> new PrivacyLegalHoldRecord(
            rs.getObject("id", UUID.class),
            rs.getString("subject_type"),
            rs.getObject("subject_id", UUID.class),
            rs.getString("reason"),
            rs.getObject("placed_by", UUID.class),
            toInstant(rs.getTimestamp("placed_at")),
            toInstant(rs.getTimestamp("released_at")),
            rs.getBoolean("active"),
            rs.getString("metadata")
    );

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    @Override
    public PrivacyRequestRecord createPrivacyRequest(
            UUID id,
            UUID userId,
            UUID studioId,
            PrivacyRequestType requestType,
            PrivacyRequestStatus status,
            Instant confirmedAt,
            String metadata
    ) {
        String sql = """
            INSERT INTO privacy_requests (id, user_id, studio_id, request_type, status, confirmed_at, metadata, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, now(), now())
            RETURNING id, user_id, studio_id, request_type, status, confirmed_at, completed_at, failure_reason, metadata::text, created_at, updated_at
        """;
        return jdbcTemplate.queryForObject(
                sql,
                requestRowMapper,
                id,
                userId,
                studioId,
                requestType.name(),
                status.name(),
                confirmedAt != null ? Timestamp.from(confirmedAt) : null,
                metadata != null ? metadata : "{}"
        );
    }

    @Override
    public Optional<PrivacyRequestRecord> findPrivacyRequestById(UUID id) {
        String sql = "SELECT id, user_id, studio_id, request_type, status, confirmed_at, completed_at, failure_reason, metadata::text, created_at, updated_at FROM privacy_requests WHERE id = ?";
        List<PrivacyRequestRecord> list = jdbcTemplate.query(sql, requestRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<PrivacyRequestRecord> findPrivacyRequestsByUserId(UUID userId) {
        String sql = "SELECT id, user_id, studio_id, request_type, status, confirmed_at, completed_at, failure_reason, metadata::text, created_at, updated_at FROM privacy_requests WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, requestRowMapper, userId);
    }

    @Override
    public List<PrivacyRequestRecord> findPrivacyRequestsByStudioId(UUID studioId) {
        String sql = "SELECT id, user_id, studio_id, request_type, status, confirmed_at, completed_at, failure_reason, metadata::text, created_at, updated_at FROM privacy_requests WHERE studio_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, requestRowMapper, studioId);
    }

    @Override
    public void updatePrivacyRequestStatus(UUID id, PrivacyRequestStatus status, Instant completedAt, String failureReason) {
        String sql = "UPDATE privacy_requests SET status = ?, completed_at = ?, failure_reason = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(
                sql,
                status.name(),
                completedAt != null ? Timestamp.from(completedAt) : null,
                failureReason,
                id
        );
    }

    @Override
    public DataExportJobRecord createDataExportJob(
            UUID id,
            UUID privacyRequestId,
            UUID userId,
            UUID studioId,
            ExportType exportType,
            DataExportStatus status
    ) {
        String sql = """
            INSERT INTO data_export_jobs (id, privacy_request_id, user_id, studio_id, export_type, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, now(), now())
            RETURNING id, privacy_request_id, user_id, studio_id, export_type, storage_key, file_size_bytes, download_token_hash, download_count, status, expires_at, completed_at, created_at, updated_at
        """;
        return jdbcTemplate.queryForObject(
                sql,
                exportJobRowMapper,
                id,
                privacyRequestId,
                userId,
                studioId,
                exportType.name(),
                status.name()
        );
    }

    @Override
    public Optional<DataExportJobRecord> findDataExportJobById(UUID id) {
        String sql = "SELECT id, privacy_request_id, user_id, studio_id, export_type, storage_key, file_size_bytes, download_token_hash, download_count, status, expires_at, completed_at, created_at, updated_at FROM data_export_jobs WHERE id = ?";
        List<DataExportJobRecord> list = jdbcTemplate.query(sql, exportJobRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<DataExportJobRecord> findLatestDataExportJob(UUID userId, ExportType exportType) {
        String sql = "SELECT id, privacy_request_id, user_id, studio_id, export_type, storage_key, file_size_bytes, download_token_hash, download_count, status, expires_at, completed_at, created_at, updated_at FROM data_export_jobs WHERE user_id = ? AND export_type = ? ORDER BY created_at DESC LIMIT 1";
        List<DataExportJobRecord> list = jdbcTemplate.query(sql, exportJobRowMapper, userId, exportType.name());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public void updateDataExportJobReady(UUID id, String storageKey, long fileSizeBytes, String downloadTokenHash, Instant expiresAt) {
        String sql = "UPDATE data_export_jobs SET status = 'READY', storage_key = ?, file_size_bytes = ?, download_token_hash = ?, expires_at = ?, completed_at = now(), updated_at = now() WHERE id = ?";
        jdbcTemplate.update(
                sql,
                storageKey,
                fileSizeBytes,
                downloadTokenHash,
                expiresAt != null ? Timestamp.from(expiresAt) : null,
                id
        );
    }

    @Override
    public void updateDataExportJobStatus(UUID id, DataExportStatus status) {
        String sql = "UPDATE data_export_jobs SET status = ?, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public void incrementDownloadCount(UUID id) {
        String sql = "UPDATE data_export_jobs SET download_count = download_count + 1, updated_at = now() WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean hasActiveLegalHold(String subjectType, UUID subjectId) {
        String sql = "SELECT COUNT(*) FROM privacy_legal_holds WHERE subject_type = ? AND subject_id = ? AND active = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, subjectType, subjectId);
        return count != null && count > 0;
    }

    @Override
    public PrivacyLegalHoldRecord createLegalHold(UUID id, String subjectType, UUID subjectId, String reason, UUID placedBy, String metadata) {
        String sql = """
            INSERT INTO privacy_legal_holds (id, subject_type, subject_id, reason, placed_by, placed_at, active, metadata)
            VALUES (?, ?, ?, ?, ?, now(), true, ?::jsonb)
            RETURNING id, subject_type, subject_id, reason, placed_by, placed_at, released_at, active, metadata::text
        """;
        return jdbcTemplate.queryForObject(
                sql,
                legalHoldRowMapper,
                id,
                subjectType,
                subjectId,
                reason,
                placedBy,
                metadata != null ? metadata : "{}"
        );
    }

    @Override
    public void releaseLegalHold(UUID id, Instant releasedAt) {
        String sql = "UPDATE privacy_legal_holds SET active = false, released_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, Timestamp.from(releasedAt != null ? releasedAt : Instant.now()), id);
    }

    @Override
    public List<PrivacyLegalHoldRecord> findActiveLegalHolds(String subjectType, UUID subjectId) {
        String sql = "SELECT id, subject_type, subject_id, reason, placed_by, placed_at, released_at, active, metadata::text FROM privacy_legal_holds WHERE subject_type = ? AND subject_id = ? AND active = true";
        return jdbcTemplate.query(sql, legalHoldRowMapper, subjectType, subjectId);
    }

    @Override
    public boolean isSoleAdminOfActiveStudio(UUID userId) {
        // A user is sole admin of an active studio if there exists any ACTIVE studio where this user is ADMIN/OWNER
        // and no OTHER active user is ADMIN/OWNER for that studio.
        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM designer_studios s
                JOIN studio_members sm ON s.id = sm.studio_id
                WHERE sm.user_id = ?
                  AND s.status = 'ACTIVE'
                  AND sm.role IN ('OWNER', 'ADMIN')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM studio_members other
                      JOIN users u ON other.user_id = u.id
                      WHERE other.studio_id = s.id
                        AND other.user_id != ?
                        AND other.role IN ('OWNER', 'ADMIN')
                        AND u.status IN ('ACTIVE', 'PENDING')
                  )
            )
        """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, userId, userId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<UUID> findActiveStudioIdsOwnedByUser(UUID userId) {
        String sql = """
            SELECT s.id
            FROM designer_studios s
            JOIN studio_members sm ON s.id = sm.studio_id
            WHERE sm.user_id = ?
              AND s.status = 'ACTIVE'
              AND sm.role IN ('OWNER', 'ADMIN')
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), userId);
    }

    @Override
    public void archiveStudioByOwner(UUID studioId, Instant closedAt) {
        String sql = """
            UPDATE designer_studios
            SET status = 'SUSPENDED',
                publication_status = 'DRAFT',
                updated_at = ?
            WHERE id = ?
        """;
        jdbcTemplate.update(sql, Timestamp.from(closedAt != null ? closedAt : Instant.now()), studioId);

        // Also unpublish active projects for this studio
        String unpublishProjectsSql = """
            UPDATE studio_projects
            SET project_status = 'ARCHIVED',
                visibility_status = 'PRIVATE',
                archived_at = ?
            WHERE studio_id = ?
        """;
        jdbcTemplate.update(unpublishProjectsSql, Timestamp.from(closedAt != null ? closedAt : Instant.now()), studioId);
    }

    @Override
    public void anonymizeUserData(UUID userId) {
        // Anonymize user identity row in users and mark as DELETED
        String anonSql = """
            UPDATE users
            SET display_name = 'Deleted User',
                email = NULL,
                phone = NULL,
                avatar_url = NULL,
                status = 'DELETED',
                deactivated_at = now(),
                deactivated_reason = 'USER_REQUESTED_CLOSURE',
                updated_at = now()
            WHERE id = ?
        """;
        jdbcTemplate.update(anonSql, userId);

        // Retain identity_external_identities binding to the tombstoned user
        // so that the same OIDC issuer+subject cannot silently provision a new account.
        // OidcService checks user status and strictly blocks DELETED users from authenticating.

        // Delete MFA credentials and recovery codes
        jdbcTemplate.update("DELETE FROM identity_mfa_methods WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM identity_recovery_codes WHERE user_id = ?", userId);

        // Terminate all sessions
        jdbcTemplate.update("DELETE FROM identity_sessions WHERE user_id = ?", userId);
    }

    @Override
    public void cleanupUserPersonalData(UUID userId) {
        // Delete user collections / moodboards
        jdbcTemplate.update("DELETE FROM user_collections WHERE user_id = ?", userId);

        // Delete notifications inbox
        jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", userId);
    }
}
