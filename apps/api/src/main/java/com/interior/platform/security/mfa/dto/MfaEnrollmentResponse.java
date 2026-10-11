package com.interior.platform.security.mfa.dto;

import java.time.Instant;

public record MfaEnrollmentResponse(
    String secret,
    String otpauthUri,
    Instant expiresAt
) {}
