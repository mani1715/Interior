package com.interior.platform.security.mfa.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaRecoveryCodeRequest(
    @NotBlank(message = "Recovery code is required")
    String recoveryCode
) {}
