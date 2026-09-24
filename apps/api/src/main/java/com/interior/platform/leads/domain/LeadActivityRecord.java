package com.interior.platform.leads.domain;

import java.time.Instant;
import java.util.UUID;

public record LeadActivityRecord(
    UUID id,
    UUID leadId,
    UUID studioId,
    UUID actorId,
    LeadActivityType activityType,
    String details,
    Instant createdAt
) {}
