package com.interior.platform.privacy.repository;

import com.interior.platform.privacy.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyRepository {
    PrivacyRequestRecord createPrivacyRequest(
            UUID id,
            UUID userId,
            UUID studioId,
            PrivacyRequestType requestType,
            PrivacyRequestStatus status,
            Instant confirmedAt,
            String metadata
    );

    Optional<PrivacyRequestRecord> findPrivacyRequestById(UUID id);

    List<PrivacyRequestRecord> findPrivacyRequestsByUserId(UUID userId);

    List<PrivacyRequestRecord> findPrivacyRequestsByStudioId(UUID studioId);

    void updatePrivacyRequestStatus(UUID id, PrivacyRequestStatus status, Instant completedAt, String failureReason);

    DataExportJobRecord createDataExportJob(
            UUID id,
            UUID privacyRequestId,
            UUID userId,
            UUID studioId,
            ExportType exportType,
            DataExportStatus status
    );

    Optional<DataExportJobRecord> findDataExportJobById(UUID id);

    Optional<DataExportJobRecord> findLatestDataExportJob(UUID userId, ExportType exportType);

    void updateDataExportJobReady(UUID id, String storageKey, long fileSizeBytes, String downloadTokenHash, Instant expiresAt);

    void updateDataExportJobStatus(UUID id, DataExportStatus status);

    void incrementDownloadCount(UUID id);

    boolean hasActiveLegalHold(String subjectType, UUID subjectId);

    PrivacyLegalHoldRecord createLegalHold(UUID id, String subjectType, UUID subjectId, String reason, UUID placedBy, String metadata);

    void releaseLegalHold(UUID id, Instant releasedAt);

    List<PrivacyLegalHoldRecord> findActiveLegalHolds(String subjectType, UUID subjectId);

    boolean isSoleAdminOfActiveStudio(UUID userId);

    List<UUID> findActiveStudioIdsOwnedByUser(UUID userId);

    void archiveStudioByOwner(UUID studioId, Instant closedAt);

    void anonymizeUserData(UUID userId);

    void cleanupUserPersonalData(UUID userId);
}
