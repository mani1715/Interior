package com.interior.platform.support.dto;

import com.interior.platform.support.domain.SupportAuthorType;

import java.time.Instant;
import java.util.UUID;

public record SupportMessageResponse(
        UUID id,
        UUID requestId,
        UUID authorUserId,
        String authorDisplayName,
        SupportAuthorType authorType,
        String body,
        boolean isInternal,
        Instant createdAt
) {}
