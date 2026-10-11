package com.interior.platform.support.dto;

import com.interior.platform.support.domain.SupportCategory;
import com.interior.platform.support.domain.SupportPriority;
import com.interior.platform.support.domain.SupportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupportRequestDetailResponse(
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
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        List<SupportMessageResponse> messages
) {}
