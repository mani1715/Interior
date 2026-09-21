package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.AiJobRecord;

public interface AiImageProvider {

    /**
     * Unique key identifying this provider implementation (e.g. "replicate", "stability", "vertex", "none").
     */
    String getProviderKey();

    /**
     * Whether this provider is actively configured with valid credentials/endpoints in the current environment.
     */
    boolean isConfigured();

    /**
     * Submits a generation request with the private input image bytes and prompt.
     */
    ProviderGenerationResponse submitGeneration(AiJobRecord job, byte[] inputImageBytes, String inputContentType);

    /**
     * Checks async status of a previously submitted generation job.
     */
    ProviderStatusResponse checkStatus(String providerJobId);

    /**
     * Cancels an in-progress generation job on the provider side if supported.
     */
    boolean cancel(String providerJobId);
}
