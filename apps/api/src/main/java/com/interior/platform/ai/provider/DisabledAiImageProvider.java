package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisabledAiImageProvider implements AiImageProvider {

    @Override
    public String getProviderKey() {
        return "none";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public boolean supportsReferenceImages() {
        return false;
    }

    @Override
    public int getMaxReferenceImages() {
        return 0;
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            List<AiGenerationReference> references
    ) {
        throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
    }

    @Override
    public ProviderStatusResponse checkStatus(String providerJobId) {
        throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
    }

    @Override
    public boolean cancel(String providerJobId) {
        return false;
    }
}
