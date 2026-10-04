package com.interior.platform.team.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResponse(
        UUID invitationId,
        String invitedEmail,
        String role,
        String rawToken,
        String invitationUrl,
        Instant expiresAt
) {}
