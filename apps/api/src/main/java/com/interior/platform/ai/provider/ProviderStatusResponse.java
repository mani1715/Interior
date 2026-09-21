package com.interior.platform.ai.provider;

public record ProviderStatusResponse(
        String providerJobId,
        boolean isCompleted,
        boolean isFailed,
        byte[] resultImageBytes,
        String resultContentType,
        String usageMetadata,
        String errorCode,
        String errorMessageSafe
) {
    public static ProviderStatusResponse processing(String providerJobId) {
        return new ProviderStatusResponse(providerJobId, false, false, null, null, null, null, null);
    }

    public static ProviderStatusResponse success(String providerJobId, byte[] imageBytes, String contentType, String usageMetadata) {
        return new ProviderStatusResponse(providerJobId, true, false, imageBytes, contentType, usageMetadata, null, null);
    }

    public static ProviderStatusResponse failure(String providerJobId, String errorCode, String errorMessageSafe) {
        return new ProviderStatusResponse(providerJobId, false, true, null, null, null, errorCode, errorMessageSafe);
    }
}
