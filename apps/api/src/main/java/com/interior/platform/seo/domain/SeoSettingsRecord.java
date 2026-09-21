package com.interior.platform.seo.domain;

import java.time.Instant;
import java.util.UUID;

public record SeoSettingsRecord(
        UUID id,
        UUID studioId,
        String metaTitleOverride,
        String metaDescriptionOverride,
        String canonicalUrlOverride,
        boolean indexingEnabled,
        Instant createdAt,
        Instant updatedAt
) {}
