package com.interior.platform.ai.dto;

import com.interior.platform.ai.domain.ReferencePurpose;

import java.time.Instant;
import java.util.UUID;

public record AiJobReferenceResponse(
        UUID id,
        UUID mediaId,
        String previewUrl,
        ReferencePurpose purpose,
        String purposeDisplayName,
        String label,
        String instruction,
        int displayOrder,
        Instant createdAt
) {}
