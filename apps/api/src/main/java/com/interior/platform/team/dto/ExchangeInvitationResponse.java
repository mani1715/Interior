package com.interior.platform.team.dto;

public record ExchangeInvitationResponse(
        boolean valid,
        String studioName,
        String maskedEmail,
        String role,
        String reason
) {}
