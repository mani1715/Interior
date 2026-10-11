package com.interior.platform.security.mfa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetMfaRequest(
    @NotBlank(message = "Reason is required for administrative MFA reset")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    String reason
) {}
