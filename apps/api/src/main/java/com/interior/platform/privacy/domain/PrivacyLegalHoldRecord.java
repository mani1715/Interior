package com.interior.platform.privacy.domain;

import java.time.Instant;
import java.util.UUID;

public record PrivacyLegalHoldRecord(
        UUID id,
        String subjectType,
        UUID subjectId,
        String reason,
        UUID placedBy,
        Instant placedAt,
        Instant releasedAt,
        boolean active,
        String metadata
) {}
