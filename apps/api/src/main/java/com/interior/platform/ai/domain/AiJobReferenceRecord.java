package com.interior.platform.ai.domain;

import java.time.Instant;
import java.util.UUID;

public record AiJobReferenceRecord(
        UUID id,
        UUID jobId,
        UUID studioId,
        UUID mediaId,
        ReferencePurpose purposeSnapshot,
        String labelSnapshot,
        String instructionSnapshot,
        int displayOrder,
        Instant createdAt
) {}
