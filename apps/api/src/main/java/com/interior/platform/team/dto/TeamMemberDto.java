package com.interior.platform.team.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberDto(
        UUID id,
        UUID userId,
        String displayName,
        String email,
        String role,
        Instant joinedAt,
        boolean isSelf,
        boolean canRemove,
        boolean canChangeRole
) {}
