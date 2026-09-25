package com.interior.platform.collections.dto;

import java.time.Instant;
import java.util.UUID;

public record UserCollectionDto(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        boolean isDefault,
        int itemCount,
        Instant createdAt,
        Instant updatedAt
) {
}
