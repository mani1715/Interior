package com.interior.platform.ai.dto;

public record AiStudioStatusResponse(
        boolean isConfigured,
        String providerKey,
        int dailyQuota,
        int usedToday,
        int remainingToday,
        boolean supportsReferenceImages,
        int maxReferenceImages,
        boolean supportsMaskEditing
) {
    public AiStudioStatusResponse(
            boolean isConfigured,
            String providerKey,
            int dailyQuota,
            int usedToday,
            int remainingToday
    ) {
        this(isConfigured, providerKey, dailyQuota, usedToday, remainingToday, true, 4, true);
    }

    public AiStudioStatusResponse(
            boolean isConfigured,
            String providerKey,
            int dailyQuota,
            int usedToday,
            int remainingToday,
            boolean supportsReferenceImages,
            int maxReferenceImages
    ) {
        this(isConfigured, providerKey, dailyQuota, usedToday, remainingToday, supportsReferenceImages, maxReferenceImages, true);
    }
}
