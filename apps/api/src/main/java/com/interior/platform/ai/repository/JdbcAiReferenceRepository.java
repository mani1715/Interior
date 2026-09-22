package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiJobReferenceRecord;
import com.interior.platform.ai.domain.AiReferenceMetadataRecord;
import com.interior.platform.ai.domain.ReferencePurpose;
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
public class JdbcAiReferenceRepository implements AiReferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiReferenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AiReferenceMetadataRecord> refMapper = (rs, rowNum) -> new AiReferenceMetadataRecord(
            getUuid(rs, "id"),
            getUuid(rs, "media_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            ReferencePurpose.valueOf(rs.getString("purpose")),
            rs.getString("label"),
            rs.getString("default_instruction"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            toInstant(rs.getTimestamp("archived_at"))
    );

    private final RowMapper<AiJobReferenceRecord> jobRefMapper = (rs, rowNum) -> new AiJobReferenceRecord(
            getUuid(rs, "id"),
            getUuid(rs, "job_id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "media_id"),
            ReferencePurpose.valueOf(rs.getString("purpose_snapshot")),
            rs.getString("label_snapshot"),
            rs.getString("instruction_snapshot"),
            rs.getInt("display_order"),
            toInstant(rs.getTimestamp("created_at"))
    );

    @Override
    public void createReference(AiReferenceMetadataRecord record) {
        String sql = "INSERT INTO ai_reference_metadata (" +
                "id, media_id, studio_id, project_id, purpose, label, default_instruction, " +
                "created_at, updated_at, archived_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                record.id(),
                record.mediaId(),
                record.studioId(),
                record.projectId(),
                record.purpose().name(),
                record.label(),
                record.defaultInstruction(),
                Timestamp.from(record.createdAt()),
                Timestamp.from(record.updatedAt()),
                record.archivedAt() != null ? Timestamp.from(record.archivedAt()) : null
        );
    }

    @Override
    public Optional<AiReferenceMetadataRecord> findById(UUID studioId, UUID referenceId) {
        String sql = "SELECT * FROM ai_reference_metadata WHERE studio_id = ? AND id = ?";
        List<AiReferenceMetadataRecord> list = jdbcTemplate.query(sql, refMapper, studioId, referenceId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<AiReferenceMetadataRecord> findByMediaId(UUID studioId, UUID mediaId) {
        String sql = "SELECT * FROM ai_reference_metadata WHERE studio_id = ? AND media_id = ?";
        List<AiReferenceMetadataRecord> list = jdbcTemplate.query(sql, refMapper, studioId, mediaId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<AiReferenceMetadataRecord> findByStudio(
            UUID studioId,
            UUID projectId,
            ReferencePurpose purpose,
            boolean includeArchived
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_reference_metadata WHERE studio_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(studioId);

        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }

        if (projectId != null) {
            sql.append(" AND (project_id = ? OR project_id IS NULL)");
            params.add(projectId);
        }

        if (purpose != null) {
            sql.append(" AND purpose = ?");
            params.add(purpose.name());
        }

        sql.append(" ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), refMapper, params.toArray());
    }

    @Override
    public void updateReference(AiReferenceMetadataRecord record) {
        String sql = "UPDATE ai_reference_metadata SET " +
                "purpose = ?, " +
                "label = ?, " +
                "default_instruction = ?, " +
                "project_id = ?, " +
                "updated_at = ? " +
                "WHERE studio_id = ? AND id = ?";

        jdbcTemplate.update(sql,
                record.purpose().name(),
                record.label(),
                record.defaultInstruction(),
                record.projectId(),
                Timestamp.from(record.updatedAt()),
                record.studioId(),
                record.id()
        );
    }

    @Override
    public void archiveReference(UUID studioId, UUID referenceId, Instant archivedAt) {
        String sql = "UPDATE ai_reference_metadata SET archived_at = ?, updated_at = ? WHERE studio_id = ? AND id = ?";
        jdbcTemplate.update(sql, Timestamp.from(archivedAt), Timestamp.from(archivedAt), studioId, referenceId);
    }

    @Override
    public void createJobReferences(List<AiJobReferenceRecord> references) {
        if (references == null || references.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO ai_job_references (" +
                "id, job_id, studio_id, media_id, purpose_snapshot, label_snapshot, " +
                "instruction_snapshot, display_order, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchParams = references.stream().map(ref -> new Object[]{
                ref.id(),
                ref.jobId(),
                ref.studioId(),
                ref.mediaId(),
                ref.purposeSnapshot().name(),
                ref.labelSnapshot(),
                ref.instructionSnapshot(),
                ref.displayOrder(),
                Timestamp.from(ref.createdAt())
        }).toList();

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    @Override
    public List<AiJobReferenceRecord> findReferencesByJobId(UUID jobId) {
        String sql = "SELECT * FROM ai_job_references WHERE job_id = ? ORDER BY display_order ASC, created_at ASC";
        return jdbcTemplate.query(sql, jobRefMapper, jobId);
    }

    @Override
    public List<AiJobReferenceRecord> findReferencesByJobIdAndStudio(UUID studioId, UUID jobId) {
        String sql = "SELECT * FROM ai_job_references WHERE studio_id = ? AND job_id = ? ORDER BY display_order ASC, created_at ASC";
        return jdbcTemplate.query(sql, jobRefMapper, studioId, jobId);
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
