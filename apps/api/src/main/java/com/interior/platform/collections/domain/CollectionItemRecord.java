package com.interior.platform.collections.domain;

import java.time.Instant;
import java.util.UUID;

public record CollectionItemRecord(
        UUID id,
        UUID collectionId,
        UUID ownerUserId,
        UUID projectId,
        String note,
        int displayOrder,
        Instant createdAt
) {
}
