package com.interior.platform.leads.dto;

import java.util.UUID;

public record LeadAssignmentRequest(
    UUID assignedUserId,
    Long expectedVersion
) {}
