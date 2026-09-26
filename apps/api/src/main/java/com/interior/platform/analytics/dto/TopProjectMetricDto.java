package com.interior.platform.analytics.dto;

import java.util.UUID;

public record TopProjectMetricDto(
        UUID projectId,
        String title,
        String slug,
        String coverImageUrl,
        long viewCount,
        long inquiryCount
) {}
