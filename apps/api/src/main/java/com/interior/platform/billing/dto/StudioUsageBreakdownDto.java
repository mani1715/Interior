package com.interior.platform.billing.dto;

public record StudioUsageBreakdownDto(
        int projectCount,
        Long projectLimit,
        int cinematicProjectCount,
        Long cinematicProjectLimit,
        boolean cinematicPortfolioAllowed,
        long storageBytesUsed,
        Long storageLimitBytes,
        long aiCreditsUsedThisMonth,
        Long aiMonthlyCreditLimit
) {}
