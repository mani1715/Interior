package com.interior.platform.operations.dto;

import com.interior.platform.operations.domain.ProviderOperationalStatus;

import java.time.Instant;
import java.util.Map;

public record ProviderMatrixResponse(
        Map<String, ProviderStatusDetail> providers,
        Instant timestamp
) {
    public record ProviderStatusDetail(
            ProviderOperationalStatus status,
            String providerName,
            String details
    ) {}
}
