package com.interior.platform.security.mfa.dto;

import java.time.Instant;

public record MfaStatusResponse(
    boolean mfaEnabled,
    String methodType,
    Instant enrolledAt,
    int recoveryCodesRemaining,
    boolean currentSessionMfaVerified,
    Instant stepUpValidUntil,
    boolean mfaRequired
) {}
