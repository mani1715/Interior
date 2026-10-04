package com.interior.platform.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(
        @NotBlank(message = "Invited email is required")
        @Email(message = "Valid email address is required")
        String email,

        @NotBlank(message = "Role is required")
        String role
) {}
