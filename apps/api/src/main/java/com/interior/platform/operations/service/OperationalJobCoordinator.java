package com.interior.platform.operations.service;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.operations.domain.OperationalJobRunRecord;
import com.interior.platform.operations.repository.PlatformOperationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Service
public class OperationalJobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(OperationalJobCoordinator.class);

    private final PlatformOperationsRepository operationsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String instanceId;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.interior.platform.common.metrics.PlatformMetrics platformMetrics;

    public OperationalJobCoordinator(PlatformOperationsRepository operationsRepository, JdbcTemplate jdbcTemplate) {
        this.operationsRepository = operationsRepository;
        this.jdbcTemplate = jdbcTemplate;
        String host = "unknown";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}
        this.instanceId = host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void setPlatformMetrics(com.interior.platform.common.metrics.PlatformMetrics platformMetrics) {
        this.platformMetrics = platformMetrics;
    }

    @FunctionalInterface
    public interface JobTask {
        int run() throws Exception;
    }

    public OperationalJobRunRecord executeJob(String jobKey, JobTask task) {
        if (!operationsRepository.tryAcquireJobLock(jobKey)) {
            log.info("Job {} is already locked by another node/process. Skipping.", jobKey);
            if (platformMetrics != null) {
                platformMetrics.recordJobExecution(jobKey, "skipped_locked");
            }
            return null;
        }

        UUID runId = UuidV7.randomUuid();
        Instant start = Instant.now();
        OperationalJobRunRecord run = new OperationalJobRunRecord(
                runId,
                jobKey,
                instanceId,
                "RUNNING",
                start,
                null,
                0,
                null,
                null
        );
        operationsRepository.recordJobRun(run);

        String jobId = "job-" + jobKey.toLowerCase() + "-" + runId.toString().substring(0, 8);
        org.slf4j.MDC.put("requestId", jobId);
        org.slf4j.MDC.put("correlationId", jobId);

        try {
            int count = task.run();
            Instant end = Instant.now();
            operationsRepository.updateJobRun(runId, "SUCCEEDED", end, count, null, null);
            log.info("Job {} succeeded: processed {} items in {}ms", jobKey, count, (end.toEpochMilli() - start.toEpochMilli()));
            if (platformMetrics != null) {
                platformMetrics.recordJobExecution(jobKey, "succeeded");
            }
            return new OperationalJobRunRecord(runId, jobKey, instanceId, "SUCCEEDED", start, end, count, null, null);
        } catch (Exception e) {
            Instant end = Instant.now();
            String failureCode = e.getClass().getSimpleName();
            String errorSummary = e.getMessage() != null && e.getMessage().length() > 255 ? e.getMessage().substring(0, 255) : e.getMessage();
            operationsRepository.updateJobRun(runId, "FAILED", end, 0, failureCode, errorSummary);
            log.error("Job {} failed: {}", jobKey, e.getMessage(), e);
            if (platformMetrics != null) {
                platformMetrics.recordJobExecution(jobKey, "failed");
            }
            return new OperationalJobRunRecord(runId, jobKey, instanceId, "FAILED", start, end, 0, failureCode, errorSummary);
        } finally {
            operationsRepository.releaseJobLock(jobKey);
            org.slf4j.MDC.clear();
        }
    }

    // --- Standard Recurring Tasks ---

    public OperationalJobRunRecord runSessionCleanup() {
        return executeJob("SESSION_CLEANUP", () -> {
            String sql = "DELETE FROM identity_sessions WHERE expires_at < now()";
            return jdbcTemplate.update(sql);
        });
    }

    public OperationalJobRunRecord runUploadExpiry() {
        return executeJob("UPLOAD_EXPIRY", () -> {
            String sql = "UPDATE upload_intents SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < now()";
            return jdbcTemplate.update(sql);
        });
    }

    public OperationalJobRunRecord runAiStuckRecovery() {
        return executeJob("AI_STUCK_RECOVERY", () -> {
            String sql = "UPDATE ai_jobs SET status = 'FAILED', error_message = 'TIMED_OUT: Job processing exceeded operational timeout' WHERE status = 'PROCESSING' AND created_at < (now() - interval '15 minutes')";
            return jdbcTemplate.update(sql);
        });
    }

    public OperationalJobRunRecord runPrivacyCleanup() {
        return executeJob("PRIVACY_CLEANUP", () -> {
            String sql = "UPDATE data_export_jobs SET status = 'EXPIRED' WHERE status = 'READY' AND expires_at < now()";
            return jdbcTemplate.update(sql);
        });
    }

    public OperationalJobRunRecord runStorageReconciliation() {
        return executeJob("STORAGE_RECONCILIATION", () -> {
            // Count uncommitted intents older than 24 hours
            String sql = "UPDATE upload_intents SET status = 'EXPIRED' WHERE status = 'PENDING' AND created_at < (now() - interval '24 hours')";
            return jdbcTemplate.update(sql);
        });
    }
}
