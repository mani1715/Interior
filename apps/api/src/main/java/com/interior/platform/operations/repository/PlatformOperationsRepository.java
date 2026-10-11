package com.interior.platform.operations.repository;

import com.interior.platform.operations.domain.OperationalJobRunRecord;
import com.interior.platform.operations.domain.PlatformOperationalStateRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformOperationsRepository {

    Optional<PlatformOperationalStateRecord> getOperationalState();

    void updateOperationalState(
            String maintenanceMode,
            String maintenanceMessage,
            Boolean uploadsEnabled,
            Boolean aiEnabled,
            Boolean registrationsEnabled,
            UUID updatedBy
    );

    OperationalJobRunRecord recordJobRun(OperationalJobRunRecord run);

    void updateJobRun(UUID runId, String status, Instant completedAt, int processedCount, String failureCode, String errorSummary);

    List<OperationalJobRunRecord> listJobRuns(int limit, int offset, String jobKey);

    boolean tryAcquireJobLock(String jobKey);

    void releaseJobLock(String jobKey);
}
