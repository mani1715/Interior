package com.interior.platform.support.dto;

import com.interior.platform.support.domain.SupportCategory;
import com.interior.platform.support.domain.SupportPriority;
import com.interior.platform.support.domain.SupportStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportRequestSummaryResponse(
        UUID id,
        UUID userId,
        String userDisplayName,
        String userEmail,
        UUID studioId,
        String studioName,
        SupportCategory category,
        String subject,
        SupportStatus status,
        SupportPriority priority,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {}
