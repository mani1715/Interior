package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.AiUsageEventRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository {

    void createJob(AiJobRecord job);

    Optional<AiJobRecord> findById(UUID studioId, UUID jobId);

    Optional<AiJobRecord> findByIdGlobal(UUID jobId);

    Optional<AiJobRecord> findByIdempotencyKey(UUID studioId, String idempotencyKey);

    List<AiJobRecord> findByStudio(UUID studioId, UUID projectId, int limit, int offset);

    int countByStudio(UUID studioId, UUID projectId);

    void updateStatus(
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
    );

    void incrementAttemptCount(UUID jobId);

    void recordUsageEvent(AiUsageEventRecord event);

    int countTodayUsage(UUID studioId);
}
