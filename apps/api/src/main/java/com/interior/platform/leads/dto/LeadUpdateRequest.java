package com.interior.platform.leads.dto;

import java.time.Instant;

public record LeadUpdateRequest(
    String status,
    String lostReason,
    Instant nextFollowUpAt,
    Long expectedVersion
) {}
