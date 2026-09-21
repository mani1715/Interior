package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import org.springframework.stereotype.Component;

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
    public ProviderGenerationResponse submitGeneration(AiJobRecord job, byte[] inputImageBytes, String inputContentType) {
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
