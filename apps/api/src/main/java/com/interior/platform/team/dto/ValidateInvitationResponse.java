package com.interior.platform.team.dto;

public record ValidateInvitationResponse(
        boolean valid,
        String studioName,
        String invitedEmail,
        String role,
        String status,
        String reason
) {}
