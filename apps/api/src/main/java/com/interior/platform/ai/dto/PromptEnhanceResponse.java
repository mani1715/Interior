package com.interior.platform.ai.dto;

import java.util.List;

public record PromptEnhanceResponse(
        String originalPrompt,
        String enhancedPrompt,
        List<String> detectedElements,
        List<String> detectedMaterials,
        List<String> detectedColors,
        List<String> detectedHardware,
        String preservationDirectives,
        String providerName
) {
}
