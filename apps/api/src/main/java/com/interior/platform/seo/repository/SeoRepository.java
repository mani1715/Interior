package com.interior.platform.seo.repository;

import com.interior.platform.seo.domain.SeoSettingsRecord;
import com.interior.platform.seo.dto.SitemapItemDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeoRepository {
    Optional<SeoSettingsRecord> findByStudioId(UUID studioId);
    void upsert(SeoSettingsRecord settings);
    void updatePublicationStatus(UUID studioId, String status, Instant publishedAt);
    List<SitemapItemDto> getPublishedStudioSitemapItems();
    List<SitemapItemDto> getPublicProjectSitemapItems();
}
