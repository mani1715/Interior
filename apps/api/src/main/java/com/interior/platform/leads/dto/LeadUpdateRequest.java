package com.interior.platform.leads.dto;

import java.time.Instant;

public record LeadUpdateRequest(
    String status,
    String lostReason,
    Instant nextFollowUpAt,
    java.util.UUID projectId,
    Long expectedVersion
) {
    public LeadUpdateRequest(String status, String lostReason, Instant nextFollowUpAt, Long expectedVersion) {
        this(status, lostReason, nextFollowUpAt, null, expectedVersion);
    }
}
