package com.interior.platform.admin.dto;

public record AdminMediaDiagnosticsDto(
        long totalCommittedBytes,
        long totalCommittedPhotos,
        long pendingUploadIntents,
        long expiredUploadIntents,
        long infectedUploadIntents,
        long failedScans,
        String scannerProvider,
        boolean scannerConfigured,
        boolean requireCleanScan
) {
    public AdminMediaDiagnosticsDto(
            long totalCommittedBytes,
            long totalCommittedPhotos,
            long pendingUploadIntents,
            long expiredUploadIntents
    ) {
        this(
                totalCommittedBytes,
                totalCommittedPhotos,
                pendingUploadIntents,
                expiredUploadIntents,
                0,
                0,
                "NOT_CONFIGURED",
                false,
                false
        );
    }
}
