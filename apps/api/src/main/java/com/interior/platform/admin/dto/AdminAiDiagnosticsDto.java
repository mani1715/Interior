package com.interior.platform.admin.dto;

public record AdminAiDiagnosticsDto(
        long totalJobs,
        long queuedJobs,
        long processingJobs,
        long completedJobs,
        long failedJobs,
        long stuckJobs,
        String providerStatus
) {}
