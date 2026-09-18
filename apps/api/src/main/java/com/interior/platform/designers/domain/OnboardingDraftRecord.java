package com.interior.platform.designers.domain;

import java.time.Instant;
import java.util.UUID;

public record OnboardingDraftRecord(
        UUID id,
        UUID userId,
        int step,
        String draftPayload,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
