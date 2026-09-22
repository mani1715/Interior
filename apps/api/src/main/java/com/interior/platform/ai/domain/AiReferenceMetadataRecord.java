package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiReferenceMetadataRecord(
        UUID id,
        UUID mediaId,
        UUID studioId,
        UUID projectId,
        ReferencePurpose purpose,
        String label,
        String defaultInstruction,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}
