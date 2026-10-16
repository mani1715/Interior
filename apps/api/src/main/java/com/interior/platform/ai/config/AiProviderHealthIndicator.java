package com.interior.platform.ai.config;

import com.interior.platform.ai.provider.AiImageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator HealthIndicator for interior AI visualizer provider.
 * Reports AI provider availability without leaking API keys or causing false DOWN
 * when AI visualizer is unconfigured or disabled.
 */
@Component
public class AiProviderHealthIndicator implements HealthIndicator {

    private final AiImageProvider aiImageProvider;

    @Autowired
    public AiProviderHealthIndicator(@Autowired(required = false) AiImageProvider aiImageProvider) {
        this.aiImageProvider = aiImageProvider;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (aiImageProvider != null) {
            boolean configured = aiImageProvider.isConfigured();
            details.put("provider", aiImageProvider.getClass().getSimpleName());
            details.put("configured", configured);
            details.put("status", configured ? "CONFIGURED" : "NOT_CONFIGURED");
            return Health.up().withDetails(details).build();
        }

        details.put("provider", "NONE");
        details.put("configured", false);
        details.put("status", "NOT_CONFIGURED");
        return Health.up().withDetails(details).build();
    }
}
