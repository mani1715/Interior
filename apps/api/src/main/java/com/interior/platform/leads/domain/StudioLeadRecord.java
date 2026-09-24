package com.interior.platform.leads.domain;

import java.time.Instant;
import java.util.UUID;

public record StudioLeadRecord(
    UUID id,
    UUID studioId,
    UUID projectId,
    LeadSource source,
    LeadStatus status,
    String name,
    String phoneNormalized,
    String emailNormalized,
    String city,
    String projectCategory,
    String budgetRange,
    String message,
    PreferredContactChannel preferredContactChannel,
    Instant contactConsentAt,
    Instant whatsappConsentAt,
    UUID assignedUserId,
    Instant nextFollowUpAt,
    String lostReason,
    boolean possibleDuplicate,
    String idempotencyKey,
    Instant createdAt,
    Instant updatedAt,
    long version,
    Instant archivedAt
) {}
