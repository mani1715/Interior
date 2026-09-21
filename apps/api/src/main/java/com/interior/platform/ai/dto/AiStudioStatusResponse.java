package com.interior.platform.ai.dto;

public record AiStudioStatusResponse(
        boolean isConfigured,
        String providerKey,
        int dailyQuota,
        int usedToday,
        int remainingToday
) {}
