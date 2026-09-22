package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.ReferencePurpose;

import java.util.UUID;

public record AiGenerationReference(
        UUID mediaId,
        ReferencePurpose purpose,
        String label,
        String instruction,
        byte[] imageBytes,
        String contentType
) {}
