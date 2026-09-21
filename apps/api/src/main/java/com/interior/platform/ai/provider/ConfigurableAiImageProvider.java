package com.interior.platform.ai.provider;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Primary
public class ConfigurableAiImageProvider implements AiImageProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableAiImageProvider.class);

    private final AiVisualizerProperties properties;
    private final HttpClient httpClient;

    public ConfigurableAiImageProvider(AiVisualizerProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                .build();
    }

    @Override
    public String getProviderKey() {
        return properties.getProvider() != null && !properties.getProvider().isBlank()
                ? properties.getProvider().toLowerCase()
                : "none";
    }

    @Override
    public boolean isConfigured() {
        String provider = properties.getProvider();
        if (provider == null || provider.isBlank() || "none".equalsIgnoreCase(provider)) {
            return false;
        }
        String key = properties.getApiKey();
        return key != null && !key.isBlank() && !key.equalsIgnoreCase("unconfigured");
    }

    @Override
    public ProviderGenerationResponse submitGeneration(AiJobRecord job, byte[] inputImageBytes, String inputContentType) {
        if (!isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }

        // Bounded, safe call to configured endpoint
        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new AiProviderNotConfiguredException("AI generation provider endpoint is missing or invalid.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(job)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // If endpoint returns binary image directly
                String ct = response.headers().firstValue("Content-Type").orElse("image/jpeg");
                if (ct.startsWith("image/")) {
                    return ProviderGenerationResponse.immediateSuccess(response.body(), ct, "{}");
                }
                // If async job ID JSON returned
                return ProviderGenerationResponse.asyncStarted("job-" + job.id(), "{}");
            } else if (response.statusCode() == 429) {
                return ProviderGenerationResponse.failure("PROVIDER_QUOTA_REACHED", "AI generation provider quota reached. Please try again later.");
            } else if (response.statusCode() >= 500) {
                return ProviderGenerationResponse.failure("PROVIDER_UNAVAILABLE", "AI generation provider temporarily unavailable (status " + response.statusCode() + ").");
            } else {
                return ProviderGenerationResponse.failure("GENERATION_REJECTED", "AI generation request rejected by provider (status " + response.statusCode() + ").");
            }
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("AI generation request timed out for job {}", job.id());
            return ProviderGenerationResponse.failure("PROVIDER_TIMEOUT", "AI generation provider timed out. Retrying may succeed.");
        } catch (Exception e) {
            log.error("AI generation provider call failed for job {}: {}", job.id(), e.getMessage());
            return ProviderGenerationResponse.failure("PROVIDER_ERROR", "AI generation provider network error.");
        }
    }

    @Override
    public ProviderStatusResponse checkStatus(String providerJobId) {
        if (!isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }
        return ProviderStatusResponse.processing(providerJobId);
    }

    @Override
    public boolean cancel(String providerJobId) {
        if (!isConfigured() || providerJobId == null) {
            return false;
        }
        return true;
    }

    private String buildRequestBody(AiJobRecord job) {
        // Safe JSON payload without prompt manipulation or script breakout
        String escapedPrompt = job.prompt()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
        return String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"num_outputs\":1}",
                properties.getModel(),
                escapedPrompt
        );
    }
}
