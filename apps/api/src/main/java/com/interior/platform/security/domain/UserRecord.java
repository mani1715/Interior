package com.interior.platform.security.domain;

import java.time.Instant;
import java.util.UUID;

public record UserRecord(
    UUID id,
    String displayName,
    String email,
    String phone,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version
) {}
