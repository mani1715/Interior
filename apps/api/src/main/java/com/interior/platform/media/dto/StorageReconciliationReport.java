package com.interior.platform.media.dto;

import java.util.UUID;

public record StorageReconciliationReport(
        UUID studioId,
        int portfolioPhotoCount,
        int pendingPhotoReservations,
        long committedStorageBytes,
        long pendingStorageBytes,
        long totalActiveStorageBytes,
        int expiredIntentsCount,
        int activeMediaCount
) {}
