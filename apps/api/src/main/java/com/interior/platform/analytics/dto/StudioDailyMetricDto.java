package com.interior.platform.analytics.dto;

import java.time.LocalDate;

public record StudioDailyMetricDto(
        LocalDate date,
        long profileViews,
        long projectViews,
        long discoveryImpressions,
        long discoveryClicks,
        long inquiriesOpened,
        long leadsCreated,
        long leadsWon,
        long reviewsSubmitted,
        long aiGenerations,
        long whatsappHandoffs
) {}
