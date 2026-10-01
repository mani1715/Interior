package com.interior.platform.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogDto(
        UUID id,
        UUID studioId,
        UUID actorId,
        String actorEmail,
        String action,
        String resourceType,
        UUID resourceId,
        String requestId,
        String details,
        Instant timestamp
) {}
