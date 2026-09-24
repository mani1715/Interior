package com.interior.platform.leads.dto;

import java.time.Instant;
import java.util.UUID;

public record LeadActivityDto(
    UUID id,
    UUID actorId,
    String actorName,
    String activityType,
    String activityDisplayName,
    String details,
    Instant createdAt
) {}
