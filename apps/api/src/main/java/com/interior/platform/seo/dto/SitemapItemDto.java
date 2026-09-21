package com.interior.platform.seo.dto;

import java.time.Instant;

public record SitemapItemDto(
        String path,
        String priority,
        String changeFreq,
        Instant lastModified,
        String coverImageUrl,
        String coverImageTitle
) {}
