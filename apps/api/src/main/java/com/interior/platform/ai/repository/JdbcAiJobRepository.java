package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.AiUsageEventRecord;
import com.interior.platform.ai.domain.EditingMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAiJobRepository implements AiJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AiJobRecord> jobMapper = (rs, rowNum) -> new AiJobRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            getUuid(rs, "input_media_id"),
            getUuid(rs, "output_media_id"),
            rs.getString("provider_key"),
            rs.getString("provider_job_id"),
            rs.getString("prompt"),
            rs.getString("system_prompt"),
            AiJobStatus.valueOf(rs.getString("status")),
            rs.getString("error_code"),
            rs.getString("error_message_safe"),
            rs.getInt("attempt_count"),
            rs.getString("idempotency_key"),
            getUuid(rs, "created_by"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("started_at")),
            toInstant(rs.getTimestamp("completed_at")),
            toInstant(rs.getTimestamp("failed_at")),
            rs.getString("usage_metadata"),
            rs.getLong("version"),
            rs.getBoolean("preserve_structure"),
            rs.getString("editing_mode") != null ? EditingMode.valueOf(rs.getString("editing_mode")) : EditingMode.FULL_IMAGE,
            rs.getString("mask_storage_key")
    );

    @Override
    public void createJob(AiJobRecord job) {
        String sql = "INSERT INTO ai_visualization_jobs (" +
                "id, studio_id, project_id, input_media_id, output_media_id, " +
                "provider_key, provider_job_id, prompt, system_prompt, status, " +
                "error_code, error_message_safe, attempt_count, idempotency_key, " +
                "created_by, created_at, started_at, completed_at, failed_at, usage_metadata, version, preserve_structure, " +
                "editing_mode, mask_storage_key" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                job.id(),
                job.studioId(),
                job.projectId(),
                job.inputMediaId(),
                job.outputMediaId(),
                job.providerKey(),
                job.providerJobId(),
                job.prompt(),
                job.systemPrompt(),
                job.status().name(),
                job.errorCode(),
                job.errorMessageSafe(),
                job.attemptCount(),
                job.idempotencyKey(),
                job.createdBy(),
                Timestamp.from(job.createdAt()),
                job.startedAt() != null ? Timestamp.from(job.startedAt()) : null,
                job.completedAt() != null ? Timestamp.from(job.completedAt()) : null,
                job.failedAt() != null ? Timestamp.from(job.failedAt()) : null,
                job.usageMetadata(),
                job.version(),
                job.preserveStructure(),
                (job.editingMode() != null ? job.editingMode() : EditingMode.FULL_IMAGE).name(),
                job.maskStorageKey()
        );
    }

    @Override
    public Optional<AiJobRecord> findById(UUID studioId, UUID jobId) {
        String sql = "SELECT * FROM ai_visualization_jobs WHERE studio_id = ? AND id = ?";
        List<AiJobRecord> list = jdbcTemplate.query(sql, jobMapper, studioId, jobId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<AiJobRecord> findByIdGlobal(UUID jobId) {
        String sql = "SELECT * FROM ai_visualization_jobs WHERE id = ?";
        List<AiJobRecord> list = jdbcTemplate.query(sql, jobMapper, jobId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<AiJobRecord> findByIdempotencyKey(UUID studioId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM ai_visualization_jobs WHERE studio_id = ? AND idempotency_key = ?";
        List<AiJobRecord> list = jdbcTemplate.query(sql, jobMapper, studioId, idempotencyKey);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<AiJobRecord> findByStudio(UUID studioId, UUID projectId, int limit, int offset) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_visualization_jobs WHERE studio_id = ?");
        params.add(studioId);

        if (projectId != null) {
            sql.append(" AND project_id = ?");
            params.add(projectId);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(Math.max(1, Math.min(limit, 100)));
        params.add(Math.max(0, offset));

        return jdbcTemplate.query(sql.toString(), jobMapper, params.toArray());
    }

    @Override
    public int countByStudio(UUID studioId, UUID projectId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM ai_visualization_jobs WHERE studio_id = ?");
        params.add(studioId);

        if (projectId != null) {
            sql.append(" AND project_id = ?");
            params.add(projectId);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public void updateStatus(
            UUID jobId,
            AiJobStatus status,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            String errorCode,
            String errorMessageSafe,
            UUID outputMediaId,
            String usageMetadata,
            long expectedVersion
    ) {
        String sql = "UPDATE ai_visualization_jobs SET " +
                "status = ?, " +
                "started_at = COALESCE(?, started_at), " +
                "completed_at = ?, " +
                "failed_at = ?, " +
                "error_code = ?, " +
                "error_message_safe = ?, " +
                "output_media_id = ?, " +
                "usage_metadata = ?, " +
                "version = version + 1 " +
                "WHERE id = ? AND version = ?";

        jdbcTemplate.update(sql,
                status.name(),
                startedAt != null ? Timestamp.from(startedAt) : null,
                completedAt != null ? Timestamp.from(completedAt) : null,
                failedAt != null ? Timestamp.from(failedAt) : null,
                errorCode,
                errorMessageSafe,
                outputMediaId,
                usageMetadata,
                jobId,
                expectedVersion
        );
    }

    @Override
    public void incrementAttemptCount(UUID jobId) {
        String sql = "UPDATE ai_visualization_jobs SET attempt_count = attempt_count + 1 WHERE id = ?";
        jdbcTemplate.update(sql, jobId);
    }

    @Override
    public void recordUsageEvent(AiUsageEventRecord event) {
        String sql = "INSERT INTO ai_usage_events (id, studio_id, job_id, event_type, provider_key, units_consumed, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                event.id(),
                event.studioId(),
                event.jobId(),
                event.eventType(),
                event.providerKey(),
                event.unitsConsumed(),
                Timestamp.from(event.createdAt())
        );
    }

    @Override
    public int countTodayUsage(UUID studioId) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        String sql = "SELECT count(*) FROM ai_usage_events WHERE studio_id = ? AND created_at >= ? AND event_type = 'GENERATION_SUCCESS'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId, Timestamp.from(startOfDay));
        return count != null ? count : 0;
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
