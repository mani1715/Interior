package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.AiJobRecord;

import java.util.List;

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
     * Whether this provider implementation supports multi-modal reference image conditioning.
     */
    default boolean supportsReferenceImages() {
        return false;
    }

    /**
     * Maximum reference images supported by this provider (typically 1 to 4).
     */
    default int getMaxReferenceImages() {
        return 0;
    }

    /**
     * Whether this provider implementation supports precision mask inpainting.
     */
    default boolean supportsMaskEditing() {
        return false;
    }

    /**
     * Submits a generation request with the private input image bytes and prompt (backwards compatibility).
     */
    default ProviderGenerationResponse submitGeneration(AiJobRecord job, byte[] inputImageBytes, String inputContentType) {
        return submitGeneration(job, inputImageBytes, inputContentType, null, null, List.of());
    }

    /**
     * Submits a generation request with the private input image bytes, prompt, and optional reference images.
     */
    ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            List<AiGenerationReference> references
    );

    /**
     * Submits a generation request with input image bytes, prompt, optional mask bytes, and optional references.
     */
    default ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            byte[] maskBytes,
            String maskContentType,
            List<AiGenerationReference> references
    ) {
        return submitGeneration(job, inputImageBytes, inputContentType, references);
    }

    /**
     * Checks async status of a previously submitted generation job.
     */
    ProviderStatusResponse checkStatus(String providerJobId);

    /**
     * Cancels an in-progress generation job on the provider side if supported.
     */
    boolean cancel(String providerJobId);
}
