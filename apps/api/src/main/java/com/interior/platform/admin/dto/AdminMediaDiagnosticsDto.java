package com.interior.platform.admin.dto;

public record AdminMediaDiagnosticsDto(
        long totalCommittedBytes,
        long totalCommittedPhotos,
        long pendingUploadIntents,
        long expiredUploadIntents
) {}
