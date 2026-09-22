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
import java.util.List;

import com.interior.platform.ai.domain.EditingMode;

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
    public boolean supportsReferenceImages() {
        return properties.isSupportsReferenceImages();
    }

    @Override
    public int getMaxReferenceImages() {
        return properties.getMaxReferenceImages();
    }

    @Override
    public boolean supportsMaskEditing() {
        return properties.isSupportsMaskEditing();
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            List<AiGenerationReference> references
    ) {
        return submitGeneration(job, inputImageBytes, inputContentType, null, null, references);
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            byte[] maskBytes,
            String maskContentType,
            List<AiGenerationReference> references
    ) {
        if (!isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }

        if (job.editingMode() == EditingMode.PRECISION_MASK && !supportsMaskEditing()) {
            return ProviderGenerationResponse.failure("MASK_EDITING_UNSUPPORTED", "The configured AI model does not support precision mask editing.");
        }

        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new AiProviderNotConfiguredException("AI generation provider endpoint is missing or invalid.");
        }

        boolean hasMask = (maskBytes != null && maskBytes.length > 0);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(job, references, hasMask)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String ct = response.headers().firstValue("Content-Type").orElse("image/jpeg");
                if (ct.startsWith("image/")) {
                    return ProviderGenerationResponse.immediateSuccess(response.body(), ct, "{}");
                }
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

    private String buildRequestBody(AiJobRecord job, List<AiGenerationReference> references, boolean hasMask) {
        String escapedPrompt = escapeJson(job.prompt());
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(properties.getModel())).append("\",");
        sb.append("\"prompt\":\"").append(escapedPrompt).append("\",");
        sb.append("\"preserve_structure\":").append(job.preserveStructure()).append(",");
        sb.append("\"editing_mode\":\"").append(job.editingMode() != null ? job.editingMode().name() : "FULL_IMAGE").append("\",");
        sb.append("\"has_mask\":").append(hasMask).append(",");
        sb.append("\"num_outputs\":1,");
        sb.append("\"references\":[");
        if (references != null && !references.isEmpty()) {
            for (int i = 0; i < references.size(); i++) {
                if (i > 0) sb.append(",");
                AiGenerationReference ref = references.get(i);
                sb.append("{");
                sb.append("\"purpose\":\"").append(ref.purpose() != null ? ref.purpose().name() : "GENERAL_STYLE").append("\",");
                sb.append("\"label\":\"").append(ref.label() != null ? escapeJson(ref.label()) : "").append("\",");
                sb.append("\"instruction\":\"").append(ref.instruction() != null ? escapeJson(ref.instruction()) : "").append("\"");
                sb.append("}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
