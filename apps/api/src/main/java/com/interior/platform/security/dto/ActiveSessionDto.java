package com.interior.platform.security.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionDto(
    UUID id,
    String deviceLabel,
    Instant authTime,
    Instant lastSeenAt,
    Instant idleExpiresAt,
    boolean current
) {}
