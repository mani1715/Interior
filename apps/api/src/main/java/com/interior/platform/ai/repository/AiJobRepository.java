package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.AiUsageEventRecord;
import com.interior.platform.ai.domain.EditingMode;

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

    List<AiJobRecord> findHistory(
            UUID studioId,
            UUID projectId,
            EditingMode editingMode,
            AiJobStatus status,
            Boolean shortlistedOnly,
            int limit,
            int offset
    );

    long countHistory(
            UUID studioId,
            UUID projectId,
            EditingMode editingMode,
            AiJobStatus status,
            Boolean shortlistedOnly
    );

    void updateShortlist(UUID studioId, UUID jobId, boolean isShortlisted);

    void updateStudioSelected(UUID studioId, UUID jobId, boolean isStudioSelected);

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
