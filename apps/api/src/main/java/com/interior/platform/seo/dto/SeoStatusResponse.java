package com.interior.platform.seo.dto;

import com.interior.platform.seo.domain.SeoChecklistItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SeoStatusResponse(
        UUID studioId,
        String studioName,
        String studioSlug,
        String publicationStatus,
        Instant publishedAt,
        String canonicalUrl,
        boolean indexingEnabled,
        String metaTitle,
        String metaDescription,
        String metaTitleOverride,
        String metaDescriptionOverride,
        String canonicalUrlOverride,
        List<SeoChecklistItem> checklist,
        boolean isPublishable,
        int totalProjectsCount,
        int publicProjectsCount,
        int totalMediaCount,
        int missingAltTextCount
) {}
