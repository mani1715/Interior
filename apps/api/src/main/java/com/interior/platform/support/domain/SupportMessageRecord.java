package com.interior.platform.support.domain;

import java.time.Instant;
import java.util.UUID;

public record SupportMessageRecord(
        UUID id,
        UUID requestId,
        UUID authorUserId,
        SupportAuthorType authorType,
        String body,
        boolean isInternal,
        Instant createdAt
) {}
