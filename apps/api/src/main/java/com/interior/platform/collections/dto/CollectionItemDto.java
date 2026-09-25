package com.interior.platform.collections.dto;

import java.time.Instant;
import java.util.UUID;

public record CollectionItemDto(
        UUID id,
        UUID collectionId,
        UUID projectId,
        String note,
        int displayOrder,
        Instant createdAt,
        boolean isAvailable,
        String projectTitle,
        String projectSlug,
        String coverImageUrl,
        String studioName,
        String studioSlug
) {
}
