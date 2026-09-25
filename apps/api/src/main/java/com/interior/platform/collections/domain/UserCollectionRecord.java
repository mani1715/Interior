package com.interior.platform.collections.domain;

import java.time.Instant;
import java.util.UUID;

public record UserCollectionRecord(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
