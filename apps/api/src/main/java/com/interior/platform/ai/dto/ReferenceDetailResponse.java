package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ReferencePurpose;

import java.time.Instant;
import java.util.UUID;

public record ReferenceDetailResponse(
        UUID id,
        UUID mediaId,
        UUID studioId,
        UUID projectId,
        ReferencePurpose purpose,
        String purposeDisplayName,
        String label,
        String defaultInstruction,
        String previewUrl,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {}
