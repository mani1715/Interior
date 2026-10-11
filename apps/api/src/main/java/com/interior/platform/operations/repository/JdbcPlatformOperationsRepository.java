package com.interior.platform.operations.repository;

import com.interior.platform.operations.domain.OperationalJobRunRecord;
import com.interior.platform.operations.domain.PlatformOperationalStateRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcPlatformOperationsRepository implements PlatformOperationsRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcPlatformOperationsRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcPlatformOperationsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PlatformOperationalStateRecord> stateRowMapper = (rs, rowNum) -> new PlatformOperationalStateRecord(
            rs.getString("id"),
            rs.getString("maintenance_mode"),
            rs.getString("maintenance_message"),
            rs.getBoolean("uploads_enabled"),
            rs.getBoolean("ai_enabled"),
            rs.getBoolean("registrations_enabled"),
            rs.getObject("updated_by", UUID.class),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final RowMapper<OperationalJobRunRecord> jobRunRowMapper = (rs, rowNum) -> {
        Timestamp compTs = rs.getTimestamp("completed_at");
        return new OperationalJobRunRecord(
                rs.getObject("id", UUID.class),
                rs.getString("job_key"),
                rs.getString("instance_id"),
                rs.getString("status"),
                rs.getTimestamp("started_at").toInstant(),
                compTs != null ? compTs.toInstant() : null,
                rs.getInt("processed_count"),
                rs.getString("failure_code"),
                rs.getString("error_summary")
        );
    };

    @Override
    public Optional<PlatformOperationalStateRecord> getOperationalState() {
        String sql = "SELECT * FROM platform_operational_state WHERE id = 'GLOBAL'";
        List<PlatformOperationalStateRecord> list = jdbcTemplate.query(sql, stateRowMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public void updateOperationalState(
            String maintenanceMode,
            String maintenanceMessage,
            Boolean uploadsEnabled,
            Boolean aiEnabled,
            Boolean registrationsEnabled,
            UUID updatedBy
    ) {
        StringBuilder sql = new StringBuilder("UPDATE platform_operational_state SET updated_at = now() ");
        List<Object> params = new ArrayList<>();

        if (maintenanceMode != null) {
            sql.append(", maintenance_mode = ? ");
            params.add(maintenanceMode);
        }
        if (maintenanceMessage != null) {
            sql.append(", maintenance_message = ? ");
            params.add(maintenanceMessage);
        }
        if (uploadsEnabled != null) {
            sql.append(", uploads_enabled = ? ");
            params.add(uploadsEnabled);
        }
        if (aiEnabled != null) {
            sql.append(", ai_enabled = ? ");
            params.add(aiEnabled);
        }
        if (registrationsEnabled != null) {
            sql.append(", registrations_enabled = ? ");
            params.add(registrationsEnabled);
        }
        if (updatedBy != null) {
            sql.append(", updated_by = ? ");
            params.add(updatedBy);
        }

        sql.append("WHERE id = 'GLOBAL'");
        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public OperationalJobRunRecord recordJobRun(OperationalJobRunRecord run) {
        String sql = """
            INSERT INTO operational_job_runs (id, job_key, instance_id, status, started_at, completed_at, processed_count, failure_code, error_summary)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                run.id(),
                run.jobKey(),
                run.instanceId(),
                run.status(),
                Timestamp.from(run.startedAt()),
                run.completedAt() != null ? Timestamp.from(run.completedAt()) : null,
                run.processedCount(),
                run.failureCode(),
                run.errorSummary()
        );
        return run;
    }

    @Override
    public void updateJobRun(UUID runId, String status, Instant completedAt, int processedCount, String failureCode, String errorSummary) {
        String sql = """
            UPDATE operational_job_runs
            SET status = ?,
                completed_at = ?,
                processed_count = ?,
                failure_code = ?,
                error_summary = ?
            WHERE id = ?
        """;
        jdbcTemplate.update(
                sql,
                status,
                completedAt != null ? Timestamp.from(completedAt) : Timestamp.from(Instant.now()),
                processedCount,
                failureCode,
                errorSummary,
                runId
        );
    }

    @Override
    public List<OperationalJobRunRecord> listJobRuns(int limit, int offset, String jobKey) {
        StringBuilder sql = new StringBuilder("SELECT * FROM operational_job_runs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (jobKey != null && !jobKey.isBlank()) {
            sql.append("AND job_key = ? ");
            params.add(jobKey.trim());
        }

        sql.append("ORDER BY started_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), jobRunRowMapper, params.toArray());
    }

    @Override
    public boolean tryAcquireJobLock(String jobKey) {
        try {
            long hash = (long) jobKey.hashCode();
            Boolean locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_lock(?)", Boolean.class, hash);
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.debug("Advisory lock not supported or failed on current dialect for job {}: {}", jobKey, e.getMessage());
            return true;
        }
    }

    @Override
    public void releaseJobLock(String jobKey) {
        try {
            long hash = (long) jobKey.hashCode();
            jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, hash);
        } catch (Exception e) {
            log.debug("Advisory unlock not supported or failed on current dialect for job {}: {}", jobKey, e.getMessage());
        }
    }
}
