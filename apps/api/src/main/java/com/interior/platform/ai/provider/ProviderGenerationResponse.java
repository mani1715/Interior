package com.interior.platform.ai.provider;

public record ProviderGenerationResponse(
        String providerJobId,
        boolean isCompletedImmediately,
        byte[] resultImageBytes,
        String resultContentType,
        String usageMetadata,
        String errorCode,
        String errorMessageSafe
) {
    public static ProviderGenerationResponse asyncStarted(String providerJobId, String usageMetadata) {
        return new ProviderGenerationResponse(providerJobId, false, null, null, usageMetadata, null, null);
    }

    public static ProviderGenerationResponse immediateSuccess(byte[] imageBytes, String contentType, String usageMetadata) {
        return new ProviderGenerationResponse(null, true, imageBytes, contentType, usageMetadata, null, null);
    }

    public static ProviderGenerationResponse failure(String errorCode, String errorMessageSafe) {
        return new ProviderGenerationResponse(null, false, null, null, null, errorCode, errorMessageSafe);
    }

    public boolean isSuccess() {
        return errorCode == null;
    }

    public boolean isRetryable() {
        return "PROVIDER_TIMEOUT".equals(errorCode) || "PROVIDER_UNAVAILABLE".equals(errorCode) || "PROVIDER_CALL_ERROR".equals(errorCode);
    }

    public byte[] imageBytes() {
        return resultImageBytes;
    }

    public String errorMessage() {
        return errorMessageSafe;
    }

    public String metadataJson() {
        return usageMetadata;
    }
}
