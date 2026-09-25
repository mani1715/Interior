package com.interior.platform.verification.repository;

import com.interior.platform.verification.domain.*;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class JdbcVerificationRepository implements VerificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcVerificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<StudioVerificationRecord> verificationMapper = (rs, rowNum) -> new StudioVerificationRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            VerificationStatus.valueOf(rs.getString("status")),
            rs.getString("business_name"),
            rs.getString("professional_type"),
            rs.getString("registration_number"),
            rs.getString("gst_number"),
            rs.getString("website_domain"),
            rs.getString("notes"),
            rs.getString("decision_reason"),
            toInstant(rs.getTimestamp("verified_at")),
            toInstant(rs.getTimestamp("expires_at")),
            rs.getString("verified_snapshot"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
    );

    private final RowMapper<VerificationDocumentRecord> documentMapper = (rs, rowNum) -> new VerificationDocumentRecord(
            getUuid(rs, "id"),
            getUuid(rs, "verification_id"),
            getUuid(rs, "studio_id"),
            VerificationDocumentType.valueOf(rs.getString("document_type")),
            rs.getString("storage_key"),
            rs.getString("original_filename"),
            rs.getString("mime_type"),
            rs.getLong("file_size_bytes"),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<VerificationEventRecord> eventMapper = (rs, rowNum) -> new VerificationEventRecord(
            getUuid(rs, "id"),
            getUuid(rs, "verification_id"),
            getUuid(rs, "studio_id"),
            VerificationEventType.valueOf(rs.getString("event_type")),
            getUuid(rs, "actor_user_id"),
            rs.getString("reason"),
            rs.getString("metadata"),
            toInstant(rs.getTimestamp("created_at"))
    );

    @Override
    public StudioVerificationRecord save(StudioVerificationRecord v) {
        Optional<StudioVerificationRecord> existing = findByStudioId(v.studioId());
        Object jsonSnapshot = toJsonbObject(v.verifiedSnapshot());

        if (existing.isPresent()) {
            String updateSql = """
                UPDATE studio_verifications SET
                    status = ?,
                    business_name = ?,
                    professional_type = ?,
                    registration_number = ?,
                    gst_number = ?,
                    website_domain = ?,
                    notes = ?,
                    decision_reason = ?,
                    verified_at = ?,
                    expires_at = ?,
                    verified_snapshot = ?,
                    updated_at = ?,
                    version = version + 1
                WHERE studio_id = ?
            """;
            jdbcTemplate.update(updateSql,
                    v.status().name(),
                    v.businessName(),
                    v.professionalType(),
                    v.registrationNumber(),
                    v.gstNumber(),
                    v.websiteDomain(),
                    v.notes(),
                    v.decisionReason(),
                    v.verifiedAt() != null ? Timestamp.from(v.verifiedAt()) : null,
                    v.expiresAt() != null ? Timestamp.from(v.expiresAt()) : null,
                    jsonSnapshot,
                    Timestamp.from(v.updatedAt()),
                    v.studioId()
            );
            return findByStudioId(v.studioId()).orElse(v);
        } else {
            String insertSql = """
                INSERT INTO studio_verifications (
                    id, studio_id, status, business_name, professional_type,
                    registration_number, gst_number, website_domain, notes,
                    decision_reason, verified_at, expires_at, verified_snapshot,
                    created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            """;
            jdbcTemplate.update(insertSql,
                    v.id(),
                    v.studioId(),
                    v.status().name(),
                    v.businessName(),
                    v.professionalType(),
                    v.registrationNumber(),
                    v.gstNumber(),
                    v.websiteDomain(),
                    v.notes(),
                    v.decisionReason(),
                    v.verifiedAt() != null ? Timestamp.from(v.verifiedAt()) : null,
                    v.expiresAt() != null ? Timestamp.from(v.expiresAt()) : null,
                    jsonSnapshot,
                    Timestamp.from(v.createdAt()),
                    Timestamp.from(v.updatedAt())
            );
            return v;
        }
    }

    @Override
    public Optional<StudioVerificationRecord> findByStudioId(UUID studioId) {
        String sql = "SELECT * FROM studio_verifications WHERE studio_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, verificationMapper, studioId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StudioVerificationRecord> findById(UUID id) {
        String sql = "SELECT * FROM studio_verifications WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, verificationMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateStatusAndDecision(
            UUID studioId,
            VerificationStatus status,
            String decisionReason,
            Instant verifiedAt,
            Instant expiresAt,
            String verifiedSnapshot
    ) {
        String sql = """
            UPDATE studio_verifications
            SET status = ?,
                decision_reason = ?,
                verified_at = ?,
                expires_at = ?,
                verified_snapshot = ?,
                updated_at = now(),
                version = version + 1
            WHERE studio_id = ?
        """;
        Object jsonSnapshot = toJsonbObject(verifiedSnapshot);

        jdbcTemplate.update(sql,
                status.name(),
                decisionReason,
                verifiedAt != null ? Timestamp.from(verifiedAt) : null,
                expiresAt != null ? Timestamp.from(expiresAt) : null,
                jsonSnapshot,
                studioId
        );
    }

    @Override
    public VerificationDocumentRecord saveDocument(VerificationDocumentRecord doc) {
        String sql = """
            INSERT INTO studio_verification_documents (
                id, verification_id, studio_id, document_type,
                storage_key, original_filename, mime_type, file_size_bytes,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                doc.id(),
                doc.verificationId(),
                doc.studioId(),
                doc.documentType().name(),
                doc.storageKey(),
                doc.originalFilename(),
                doc.mimeType(),
                doc.fileSizeBytes(),
                Timestamp.from(doc.createdAt())
        );
        return doc;
    }

    @Override
    public List<VerificationDocumentRecord> listDocuments(UUID studioId) {
        String sql = "SELECT * FROM studio_verification_documents WHERE studio_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, documentMapper, studioId);
    }

    @Override
    public void deleteDocument(UUID studioId, UUID documentId) {
        String sql = "DELETE FROM studio_verification_documents WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, studioId, documentId);
    }

    @Override
    public VerificationEventRecord saveEvent(VerificationEventRecord event) {
        String sql = """
            INSERT INTO studio_verification_events (
                id, verification_id, studio_id, event_type,
                actor_user_id, reason, metadata, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        Object jsonMeta = toJsonbObject(event.metadata());

        jdbcTemplate.update(sql,
                event.id(),
                event.verificationId(),
                event.studioId(),
                event.eventType().name(),
                event.actorUserId(),
                event.reason(),
                jsonMeta,
                Timestamp.from(event.createdAt())
        );
        return event;
    }

    @Override
    public List<VerificationEventRecord> listEvents(UUID studioId) {
        String sql = "SELECT * FROM studio_verification_events WHERE studio_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, eventMapper, studioId);
    }

    private Boolean isPostgres = null;

    private boolean isPostgreSql() {
        if (isPostgres == null) {
            try {
                if (jdbcTemplate.getDataSource() != null) {
                    try (java.sql.Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                        String name = conn.getMetaData().getDatabaseProductName();
                        isPostgres = name != null && name.toLowerCase().contains("postgres");
                    }
                } else {
                    isPostgres = false;
                }
            } catch (Exception e) {
                isPostgres = false;
            }
        }
        return Boolean.TRUE.equals(isPostgres);
    }

    private Object toJsonbObject(String jsonString) {
        if (jsonString == null) return null;
        if (!isPostgreSql()) {
            return jsonString;
        }
        try {
            Class<?> clazz = Class.forName("org.postgresql.util.PGobject");
            Object pgo = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setType", String.class).invoke(pgo, "jsonb");
            clazz.getMethod("setValue", String.class).invoke(pgo, jsonString);
            return pgo;
        } catch (Exception ignored) {
            return jsonString;
        }
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
