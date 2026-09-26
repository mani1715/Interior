package com.interior.platform.analytics.dto;

import com.interior.platform.analytics.domain.AnalyticsEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PublicAnalyticsEventRequest(
        @NotNull AnalyticsEventType eventType,
        @Size(max = 32) String entityType,
        @Size(max = 128) String entitySlug,
        @Size(max = 64) String sessionHash,
        @Size(max = 512) String referrer,
        @Size(max = 32) String deviceClass,
        Map<String, Object> metadata
) {}
