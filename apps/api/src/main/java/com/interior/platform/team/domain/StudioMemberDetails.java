package com.interior.platform.team.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioMemberDetails(
        UUID membershipId,
        UUID studioId,
        UUID userId,
        String displayName,
        String email,
        String role,
        Instant joinedAt
) {}
