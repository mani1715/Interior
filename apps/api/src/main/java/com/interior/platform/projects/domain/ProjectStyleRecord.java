package com.interior.platform.projects.domain;

import java.time.Instant;
import java.util.UUID;

public record ProjectStyleRecord(
        UUID id,
        UUID projectId,
        UUID studioId,
        ProjectStyle styleCode,
        Instant createdAt
) {}
