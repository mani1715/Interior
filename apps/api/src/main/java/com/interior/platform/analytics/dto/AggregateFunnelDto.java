package com.interior.platform.analytics.dto;

public record AggregateFunnelDto(
        long publicViews,
        long inquiriesOpened,
        long leadsCreated,
        long leadsWon,
        double inquiryOpenRatePercent,
        double leadConversionRatePercent,
        double leadWinRatePercent
) {}
