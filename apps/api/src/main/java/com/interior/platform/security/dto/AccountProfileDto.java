package com.interior.platform.security.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AccountProfileDto(
    UUID id,
    String displayName,
    String email,
    String phone,
    String avatarUrl,
    String status,
    Instant createdAt,
    Instant updatedAt,
    Set<String> roles,
    List<StudioSummaryDto> studios,
    Instant deactivatedAt,
    Instant deletionRequestedAt
) {
    public record StudioSummaryDto(
        UUID studioId,
        String studioName,
        String studioSlug,
        String role
    ) {}
}
