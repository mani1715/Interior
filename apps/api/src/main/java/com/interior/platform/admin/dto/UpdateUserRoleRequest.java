package com.interior.platform.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "CUSTOMER|DESIGNER|DESIGNER_TEAM|ADMIN|SUPER_ADMIN",
                 message = "Role must be CUSTOMER, DESIGNER, DESIGNER_TEAM, ADMIN, or SUPER_ADMIN")
        String role
) {}
