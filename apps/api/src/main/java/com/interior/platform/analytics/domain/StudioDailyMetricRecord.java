package com.interior.platform.analytics.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudioDailyMetricRecord(
        UUID studioId,
        LocalDate metricDate,
        long profileViews,
        long projectViews,
        long discoveryImpressions,
        long discoveryClicks,
        long inquiriesOpened,
        long leadsCreated,
        long leadsWon,
        long reviewsSubmitted,
        long aiGenerations,
        long whatsappHandoffs,
        Instant createdAt,
        Instant updatedAt
) {}
