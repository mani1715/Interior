package com.interior.platform.privacy.dto;

import java.time.Instant;
import java.util.UUID;

public record LegalHoldDto(
        UUID id,
        String subjectType,
        UUID subjectId,
        String reason,
        UUID placedBy,
        Instant placedAt,
        Instant releasedAt,
        boolean active
) {}
