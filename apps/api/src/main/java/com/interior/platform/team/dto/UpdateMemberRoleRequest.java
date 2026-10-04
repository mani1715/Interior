package com.interior.platform.team.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberRoleRequest(
        @NotBlank(message = "Role is required")
        String role
) {}
