package com.interior.platform.team.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
        @NotBlank(message = "Token is required")
        String token
) {}
