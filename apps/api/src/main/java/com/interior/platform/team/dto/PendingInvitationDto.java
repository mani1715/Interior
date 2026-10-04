package com.interior.platform.team.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationDto(
        UUID id,
        String invitedEmail,
        String role,
        String status,
        Instant createdAt,
        Instant expiresAt
) {}
