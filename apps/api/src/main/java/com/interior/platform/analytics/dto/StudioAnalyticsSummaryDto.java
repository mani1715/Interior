package com.interior.platform.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudioAnalyticsSummaryDto(
        UUID studioId,
        LocalDate startDate,
        LocalDate endDate,
        String trackingSince,
        long totalProfileViews,
        long totalProjectViews,
        long totalDiscoveryImpressions,
        long totalDiscoveryClicks,
        long totalInquiriesOpened,
        long totalLeadsCreated,
        long totalLeadsWon,
        long totalReviewsSubmitted,
        long totalAiGenerations,
        long totalWhatsappHandoffs,
        AggregateFunnelDto funnel,
        List<TopProjectMetricDto> topProjects,
        List<StudioDailyMetricDto> timeseries
) {}
