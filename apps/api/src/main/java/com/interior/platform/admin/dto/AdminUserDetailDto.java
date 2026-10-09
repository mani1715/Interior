package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AdminUserDetailDto(
        UUID id,
        String displayName,
        String email,
        String phone,
        String status,
        String suspensionReason,
        Instant createdAt,
        Set<String> roles,
        List<String> studioMemberships,
        int activeSessionsCount
) {}
