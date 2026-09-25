package com.interior.platform.verification.repository;

import com.interior.platform.verification.domain.StudioVerificationRecord;
import com.interior.platform.verification.domain.VerificationDocumentRecord;
import com.interior.platform.verification.domain.VerificationEventRecord;
import com.interior.platform.verification.domain.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationRepository {

    StudioVerificationRecord save(StudioVerificationRecord verification);

    Optional<StudioVerificationRecord> findByStudioId(UUID studioId);

    Optional<StudioVerificationRecord> findById(UUID id);

    void updateStatusAndDecision(
            UUID studioId,
            VerificationStatus status,
            String decisionReason,
            Instant verifiedAt,
            Instant expiresAt,
            String verifiedSnapshot
    );

    VerificationDocumentRecord saveDocument(VerificationDocumentRecord doc);

    List<VerificationDocumentRecord> listDocuments(UUID studioId);

    void deleteDocument(UUID studioId, UUID documentId);

    VerificationEventRecord saveEvent(VerificationEventRecord event);

    List<VerificationEventRecord> listEvents(UUID studioId);
}
