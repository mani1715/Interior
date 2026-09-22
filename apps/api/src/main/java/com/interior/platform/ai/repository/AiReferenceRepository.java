package com.interior.platform.ai.repository;

import com.interior.platform.ai.domain.AiJobReferenceRecord;
import com.interior.platform.ai.domain.AiReferenceMetadataRecord;
import com.interior.platform.ai.domain.ReferencePurpose;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiReferenceRepository {

    void createReference(AiReferenceMetadataRecord record);

    Optional<AiReferenceMetadataRecord> findById(UUID studioId, UUID referenceId);

    Optional<AiReferenceMetadataRecord> findByMediaId(UUID studioId, UUID mediaId);

    List<AiReferenceMetadataRecord> findByStudio(
            UUID studioId,
            UUID projectId,
            ReferencePurpose purpose,
            boolean includeArchived
    );

    void updateReference(AiReferenceMetadataRecord record);

    void archiveReference(UUID studioId, UUID referenceId, Instant archivedAt);

    void createJobReferences(List<AiJobReferenceRecord> references);

    List<AiJobReferenceRecord> findReferencesByJobId(UUID jobId);

    List<AiJobReferenceRecord> findReferencesByJobIdAndStudio(UUID studioId, UUID jobId);
}
