package com.interior.platform.leads.dto;

import java.time.Instant;
import java.util.UUID;

public record LeadSummaryDto(
    UUID id,
    UUID studioId,
    UUID projectId,
    String projectTitle,
    String projectSlug,
    String source,
    String sourceDisplayName,
    String status,
    String statusDisplayName,
    String name,
    String phoneMasked,
    String phoneNormalized,
    String emailNormalized,
    String city,
    String projectCategory,
    String budgetRange,
    String preferredContactChannel,
    boolean hasWhatsappConsent,
    UUID assignedUserId,
    String assignedUserName,
    Instant nextFollowUpAt,
    boolean possibleDuplicate,
    Instant createdAt,
    Instant updatedAt,
    long version,
    Instant archivedAt
) {}
