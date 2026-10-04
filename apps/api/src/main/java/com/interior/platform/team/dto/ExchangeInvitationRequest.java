package com.interior.platform.team.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeInvitationRequest(
        @NotBlank(message = "Token is required")
        String token
) {}
