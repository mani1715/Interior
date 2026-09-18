package com.interior.platform.security.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioMemberRecord(
    UUID id,
    UUID studioId,
    String studioName,
    String studioSlug,
    UUID userId,
    String role,
    Instant grantedAt
) {}
